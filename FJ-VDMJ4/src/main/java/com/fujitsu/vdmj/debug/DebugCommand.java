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
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ParserException;

/**
 * Class to process one debugger command.
 */
public class DebugCommand
{
	/** The breakpoint which caused us to stop. */
	private final Breakpoint breakpoint;
	/** The context that was active when the breakpoint stopped. */
	private final Context ctxt;
	/** The interpreter */
	private final Interpreter interpreter;
	
	/** The before/after printing of the source command. */
	private static final int SOURCE_LINES = 5;

	/** The number of stack levels moved down. */
	private int frame = 0;

	public DebugCommand(Breakpoint breakpoint, Context ctxt)
	{
		this.breakpoint = breakpoint;
		this.ctxt = ctxt;
		this.interpreter = Interpreter.getInstance();
	}

	/**
	 * Perform one debugger command
	 */
	public String run(String line)
	{
   		try
		{
			interpreter.setDefaultName(breakpoint.location.module);
		}
		catch (Exception e)
		{
			// throw new InternalException(52, "Cannot set default name at breakpoint");
		}
   		
   		String result = "";

   		try
   		{
			if (line.equals("quit") || line.equals("q"))
			{
				result = doQuit();
			}
			else if (line.equals("stop"))
			{
				result = doStop();
			}
			else if (line.equals("help") || line.equals("?"))
			{
				result = doHelp();
			}
			else if(line.equals("continue") || line.equals("c"))
			{
				result = doContinue();
			}
			else if(line.equals("stack"))
			{
				result = doStack();
			}
			else if(line.equals("up"))
			{
				result = doUp();
			}
			else if(line.equals("down"))
			{
				result = doDown();
			}
			else if(line.equals("step") || line.equals("s"))
			{
				result = doStep();
			}
			else if(line.equals("next") || line.equals("n"))
			{
				result = doNext();
			}
			else if(line.equals("out") || line.equals("o"))
			{
				result = doOut();
			}
			else if(line.equals("source"))
			{
				result = doSource();
			}
			else if (line.startsWith("print ") || line.startsWith("p "))
			{
				try
				{
					ctxt.threadState.setAtomic(true);
					result = doEvaluate(line);
				}
				finally
				{
					ctxt.threadState.setAtomic(false);
				}
			}
			else
			{
				result = "Bad command. Try 'help'";
			}
		}
		catch (Exception e)
		{
			result = doException(e);
		}

		return result;
	}

	protected String doException(Exception e)
	{
		while (e instanceof InvocationTargetException)
		{
			e = (Exception)e.getCause();
		}
		
		return "Exception: " + e.getMessage();
	}

	/**
	 * Evaluate an expression in the breakpoint's context. This is similar
	 * to the superclass method, except that the context is the one taken
	 * from the breakpoint, and the execution is not timed.
	 *
	 * @see com.fujitsu.vdmj.commands.CommandReader#doEvaluate(java.lang.String)
	 */
	private String doEvaluate(String line)
	{
		line = line.substring(line.indexOf(' ') + 1);

		try
		{
   			return line + " = " + interpreter.evaluate(line, getFrame());
		}
		catch (ParserException e)
		{
			return "Syntax: " + e;
		}
		catch (ContextException e)
		{
			return "Runtime: " + e.getMessage();
		}
		catch (RuntimeException e)
		{
			return "Runtime: " + e.getMessage();
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			return "Error: " + e.getMessage();
		}
	}

	private String doStep()
	{
   		ctxt.threadState.setBreaks(breakpoint.location, null, null);
   		return "resume";
	}

	private String doNext()
	{
		ctxt.threadState.setBreaks(breakpoint.location,	ctxt.getRoot(), null);
		return "resume";
	}

	private String doOut()
	{
		ctxt.threadState.setBreaks(breakpoint.location, null, ctxt.getRoot().outer);
		return "resume";
	}

	private String doContinue()
	{
		ctxt.threadState.setBreaks(null, null, null);
		return "resume";
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

	private String doStack()
	{
		StringBuilder sb = new StringBuilder(); 
		sb.append(breakpoint.stoppedAtString());
		sb.append("\n");
		
		StringWriter sw = new StringWriter();
		getFrame().printStackTrace(new PrintWriter(sw), true);
		sb.append(sw.toString());
		
		return sb.toString();
	}

	private String doUp()
	{
		if (frame == 0)
		{
			return "Already at first frame";
		}
		else
		{
			frame--;
			Context fp = getFrame();
			return "In context of " + fp.title + " " + fp.location;
		}
	}

	private String doDown()
	{
		Context fp = getFrame();

		if (fp.outer == null)
		{
			return "Already at last frame";
		}
		else
		{
			frame++;
			fp = getFrame();
			return "In context of " + fp.title + " " + fp.location;
		}
	}

	private String doSource()
	{
		LexLocation loc = (frame == 0) ? breakpoint.location : getFrame().location;
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

		return sb.toString();
	}

	private String doStop()
	{
		return "quit";
	}

	private String doQuit()
	{
		return "quit";
	}

	private String doHelp()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("step - step one expression/statement");
		sb.append("next - step over functions or operations");
		sb.append("out - run to the return of functions or operations");
		sb.append("continue - resume execution");
		sb.append("stack - display the current stack frame context");
		sb.append("up - move the stack frame context up one frame");
		sb.append("down - move the stack frame context down one frame");
		sb.append("source - list VDM source code around the current breakpoint");
		sb.append("stop - terminate the execution immediately");
		sb.append("threads - list active threads");
		
		return sb.toString();
	}
}
