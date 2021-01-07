/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.debug;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.ConsolePrintWriter;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ParserException;

/**
 * Class to process one debugger command.
 */
public class ConsoleDebugExecutor implements DebugExecutor
{
	/** The location where the thread stopped. */
	private final LexLocation breakloc;
	/** The context that was active when the thread stopped. */
	private final Context ctxt;
	/** The interpreter */
	private final Interpreter interpreter;
	
	/** The before/after printing of the source command. */
	private static final int SOURCE_LINES = 5;

	/** The number of stack levels moved down. */
	private int frame = 0;

	public ConsoleDebugExecutor(LexLocation breakloc, Context ctxt)
	{
		this.interpreter = Interpreter.getInstance();
		this.breakloc = breakloc;
		this.ctxt = ctxt;
	}

	/**
	 * Perform one debugger command
	 */
	@Override
	public DebugCommand run(DebugCommand request)
	{
   		try
		{
			interpreter.setDefaultName(breakloc.module);
		}
		catch (Exception e)
		{
			throw new InternalException(52, "Cannot set default name at breakpoint");
		}
   		
   		DebugCommand result = null;

   		try
   		{
			switch (request.getType())
			{
				case QUIT:
					result = doQuit();
					break;
					
				case STOP:
					result = doStop();
					break;
					
				case HELP:
					result = doHelp();
					break;
					
				case CONTINUE:
					result = doContinue();
					break;
					
				case STACK:
					result = doStack();
					break;
					
				case UP:
					result = doUp();
					break;
					
				case DOWN:
					result = doDown();
					break;
					
				case STEP:
					result = doStep();
					break;
					
				case NEXT:
					result = doNext();
					break;
					
				case OUT:
					result = doOut();
					break;
					
				case SOURCE:
					result = doSource();
					break;
					
				case PRINT:
					result = doEvaluate(request);
					break;
					
				default:
					result = new DebugCommand(DebugType.ERROR, "Bad command. Try 'help'");
					break;
			}
		}
		catch (Exception e)
		{
			result = new DebugCommand(DebugType.ERROR, e);
		}

		return result;
	}

	/**
	 * Evaluate an expression in the context, atomically.
	 */
	private DebugCommand doEvaluate(DebugCommand line)
	{
		String expr = (String)line.getPayload();

		try
		{
			ctxt.threadState.setAtomic(true);
   			return new DebugCommand(DebugType.PRINT, expr + " = " + interpreter.evaluate(expr, getFrame()));
		}
		catch (ParserException e)
		{
			return new DebugCommand(DebugType.ERROR, "Syntax: " + e);
		}
		catch (ContextException e)
		{
			return new DebugCommand(DebugType.ERROR, "Runtime: " + e.getMessage());
		}
		catch (RuntimeException e)
		{
			return new DebugCommand(DebugType.ERROR, "Runtime: " + e.getMessage());
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			return new DebugCommand(DebugType.ERROR, "Error: " + e.getMessage());
		}
		finally
		{
			ctxt.threadState.setAtomic(false);
		}
	}

	private DebugCommand doStep()
	{
   		ctxt.threadState.setBreaks(breakloc, null, null);
   		return DebugCommand.RESUME;
	}

	private DebugCommand doNext()
	{
		ctxt.threadState.setBreaks(breakloc, ctxt.getRoot(), null);
   		return DebugCommand.RESUME;
	}

	private DebugCommand doOut()
	{
		ctxt.threadState.setBreaks(breakloc, null, ctxt.getRoot().outer);
   		return DebugCommand.RESUME;
	}

	private DebugCommand doContinue()
	{
		ctxt.threadState.setBreaks(null, null, null);
   		return DebugCommand.RESUME;
	}

	private DebugCommand doStack()
	{
		StringBuilder sb = new StringBuilder(); 
		sb.append("Stopped [" + Thread.currentThread().getName() + "] " + breakloc);
		sb.append("\n");
		
		StringWriter sw = new StringWriter();
		getFrame().printStackTrace(new ConsolePrintWriter(new PrintWriter(sw)), true);
		sb.append(sw.toString());
		
		return new DebugCommand(DebugType.STACK, sb.toString());
	}

	private DebugCommand doUp()
	{
		if (frame == 0)
		{
			return new DebugCommand(DebugType.UP, "Already at first frame");
		}
		else
		{
			frame--;
			Context fp = getFrame();
			return new DebugCommand(DebugType.UP, "In context of " + fp.title + " " + fp.location);
		}
	}

	private DebugCommand doDown()
	{
		Context fp = getFrame();

		if (fp.outer == null)
		{
			return new DebugCommand(DebugType.DOWN, "Already at last frame");
		}
		else
		{
			frame++;
			fp = getFrame();
			return new DebugCommand(DebugType.DOWN, "In context of " + fp.title + " " + fp.location);
		}
	}

	private DebugCommand doSource()
	{
		LexLocation loc = (frame == 0) ? breakloc : getFrame().location;
		
		if (loc.module.equals("?"))
		{
			return new DebugCommand(DebugType.ERROR, "No source");
		}
		
		File file = loc.file;
		int current = loc.startLine;

		int start = current - SOURCE_LINES;
		if (start < 1) start = 1;
		int end = start + SOURCE_LINES*2 + 1;
		
		StringBuilder sb = new StringBuilder();

		for (int src = start; src < end; src++)
		{
			sb.append(interpreter.getSourceLine(file, src, (src == current) ? ":>>" : ":  "));
			sb.append("\n");
		}

		return new DebugCommand(DebugType.SOURCE, sb.toString());
	}

	private DebugCommand doStop()
	{
		return DebugCommand.STOP;
	}

	private DebugCommand doQuit()
	{
		return DebugCommand.QUIT;
	}

	private DebugCommand doHelp()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("step - step one expression/statement\n");
		sb.append("next - step over functions or operations\n");
		sb.append("out - run to the return of functions or operations\n");
		sb.append("continue - resume execution of all threads\n");
		sb.append("stack - display the current stack frame context\n");
		sb.append("up - move the stack frame context up one frame\n");
		sb.append("down - move the stack frame context down one frame\n");
		sb.append("source - list VDM source code around the current breakpoint\n");
		sb.append("stop - terminate the execution immediately\n");
		sb.append("threads - list active threads\n");
		sb.append("thread <n> - select active thread to debug\n");
		sb.append("break [<file>:]<line#> [<condition>] - create a breakpoint\n");
		sb.append("break <function/operation> [<condition>] - create a breakpoint\n");
		sb.append("trace [<file>:]<line#> [<exp>] - create a tracepoint\n");
		sb.append("trace <function/operation> [<exp>] - create a tracepoint\n");
		sb.append("remove <breakpoint#> - remove a trace/breakpoint\n");
		sb.append("list - list breakpoints\n");
		
		return new DebugCommand(DebugType.HELP, sb.toString());
	}

	private Context getFrame()
	{
		Context fp = ctxt;
		int c = frame;
	
		while (c > 0 && fp.outer != null)
		{
			fp = fp.outer;
			c--;
		}
	
		return fp;
	}

	@Override
	public void clear()
	{
		// Nothing to clear
	}
}
