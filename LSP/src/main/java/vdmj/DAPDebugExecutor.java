/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package vdmj;

import java.lang.reflect.InvocationTargetException;

import com.fujitsu.vdmj.debug.DebugCommand;
import com.fujitsu.vdmj.debug.DebugExecutor;
import com.fujitsu.vdmj.debug.DebugType;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.syntax.ParserException;

import json.JSONArray;
import json.JSONObject;

public class DAPDebugExecutor implements DebugExecutor
{
	/** The interpreter */
	private final Interpreter interpreter;

	/** The location where the thread stopped. */
	private LexLocation breakloc;
	/** The context that was active when the thread stopped. */
	private Context ctxt;

	public DAPDebugExecutor()
	{
		interpreter = Interpreter.getInstance();
	}

	@Override
	public void setBreakpoint(LexLocation breakloc, Context ctxt)
	{
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
					
				case CONTINUE:
					result = doContinue();
					break;
					
				case STACK:
					result = doStack(request);
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
					
				case PRINT:
					result = doEvaluate(request);
					break;
					
				default:
					result = new DebugCommand(DebugType.ERROR, "Unsupported debug command");
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
	private DebugCommand doEvaluate(DebugCommand command)
	{
		String expr = (String)command.getPayload();

		try
		{
			ctxt.threadState.setAtomic(true);
   			return new DebugCommand(DebugType.DATA, expr + " = " + interpreter.evaluate(expr, ctxt));
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

	private DebugCommand doStack(DebugCommand command)
	{
		JSONObject arguments = (JSONObject) command.getPayload();
		long startFrame = arguments.get("startFrame");
		long levels = arguments.get("levels");
		
		Context c = ctxt;
		int totalFrames = 1;
		
		while (c.outer != null)
		{
			c = c.outer;
			totalFrames++;
		}
		
		JSONObject stackResponse = null;
		
		if (startFrame >= totalFrames)	// Not enough frames for startFrame?
		{
			stackResponse = new JSONObject("stackFrames", new JSONArray(), "totalFrames", totalFrames);
		}
		else
		{
			c = ctxt;
			int frame = 0;
			
			while (frame < startFrame && c.outer != null)
			{
				c = c.outer;
				frame++;
			}
			
			JSONArray frames = new JSONArray();
			
			for (; frame < totalFrames && frame < levels; frame++)
			{
				frames.add(stackFrame(c));
				c = c.outer;
			}
			
			stackResponse = new JSONObject("stackFrames", frames, "totalFrames", totalFrames);
		}
		
		return new DebugCommand(DebugType.STACK, stackResponse);
	}

	private JSONObject stackFrame(Context frame)
	{
		return new JSONObject(
			"id",		frame.threadState.threadId,
			"name",		frame.title,
			"line",		frame.location.startLine,
			"column",	frame.location.startPos,
			"moduleId",	frame.location.module);
	}

	private DebugCommand doStop()
	{
		return DebugCommand.STOP;
	}

	private DebugCommand doQuit()
	{
		return DebugCommand.QUIT;
	}
}
