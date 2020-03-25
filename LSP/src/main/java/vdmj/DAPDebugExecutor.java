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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fujitsu.vdmj.debug.DebugCommand;
import com.fujitsu.vdmj.debug.DebugExecutor;
import com.fujitsu.vdmj.debug.DebugType;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

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

	private Map<Integer, Set<Integer>> framesToVarReferences;
	private Map<Integer, Context> variablesReferences;
	private int nextFrameId;
	private int nextVariablesReference;


	public DAPDebugExecutor()
	{
		interpreter = Interpreter.getInstance();
	}

	@Override
	public void setBreakpoint(LexLocation breakloc, Context ctxt)
	{
		this.breakloc = breakloc;
		this.ctxt = ctxt;
		
		buildCache();
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
					
				case STACK:	// frames
					result = doStack(request);
					break;
					
				case DATA:	// scopes and variablesReferences
					result = doData(request);
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
		
		int totalFrames = framesToVarReferences.size();
		JSONObject stackResponse = null;
		
		if (startFrame >= totalFrames)	// Not enough frames for startFrame?
		{
			stackResponse = new JSONObject("stackFrames", new JSONArray(), "totalFrames", totalFrames);
		}
		else
		{
			JSONArray frames = new JSONArray();
			
			for (int frameId = (int) startFrame; frameId < totalFrames; frameId++)
			{
				Set<Integer> vrefs = framesToVarReferences.get(frameId);
				Integer vref = vrefs.iterator().next();		// Any one
				Context frame = variablesReferences.get(vref);
				
				frames.add(stackFrame(frameId, frame));
				if (frames.size() >= levels) break;
			}
			
			stackResponse = new JSONObject("stackFrames", frames, "totalFrames", totalFrames);
		}
		
		return new DebugCommand(DebugType.STACK, stackResponse);
	}

	private JSONObject stackFrame(int frameId, Context frame)
	{
		LexLocation floc = frame.location;
		
		return new JSONObject(
			"id",		frameId,
			"name",		frame.title,
			"source",	new JSONObject("path", floc.file.getAbsolutePath()),
			"line",		floc.startLine,
			"column",	floc.startPos,
			"moduleId",	floc.module);
	}

	private DebugCommand doData(DebugCommand command)
	{
		JSONObject arguments = (JSONObject) command.getPayload();
		if (arguments.containsKey("frameId"))
		{
			return doScopes(command);
		}
		else
		{
			return doVariables(command);
		}
	}
	
	private DebugCommand doScopes(DebugCommand command)
	{
		JSONObject arguments = (JSONObject) command.getPayload();
		long frameId = arguments.get("frameId");
		JSONArray scopes = new JSONArray();
		
		for (Integer vref: framesToVarReferences.get((int)frameId))
		{
			Context vars = variablesReferences.get(vref);
			
			scopes.add(new JSONObject(
				"name", vars.title,
				"presentationHint", vars.title,
				"variablesReference", vref,
				"namedVariables", vars.size(),
				"source", new JSONObject("path", vars.location.file.getAbsolutePath())
			));
		}
		
		return new DebugCommand(DebugType.STACK, new JSONObject("scopes", scopes));
	}

	private DebugCommand doVariables(DebugCommand command)
	{
		JSONObject arguments = (JSONObject) command.getPayload();
		long vref = arguments.get("variablesReference");
		Context vars = variablesReferences.get((int)vref);
		JSONArray variables = new JSONArray();
		
		for (TCNameToken name: vars.keySet())
		{
			variables.add(new JSONObject(
				"name", name.toString(),
				"value", vars.get(name).toString(),
				"variablesReference", vref
			));
		}
		
		return new DebugCommand(DebugType.STACK, new JSONObject("variablesReferences", variables));
	}

	private DebugCommand doStop()
	{
		return DebugCommand.STOP;
	}

	private DebugCommand doQuit()
	{
		return DebugCommand.QUIT;
	}
	
	private void buildCache()
	{
		framesToVarReferences = new HashMap<Integer, Set<Integer>>();	// frameId to variablesReferences
		variablesReferences = new HashMap<Integer, Context>();			// variablesReferences to name/values
		
		nextFrameId = 0;
		nextVariablesReference = 0;
		
		Context c = ctxt;
		LexLocation[] frameLoc = new LexLocation[1];
		frameLoc[0] = breakloc;	// [0] updated by buildScopes
		
		while (c != null)
		{
			framesToVarReferences.put(nextFrameId, new HashSet<Integer>());
			c = buildScopes(c, frameLoc);
			nextFrameId++;
		}
	}
	
	private Context buildScopes(Context c, LexLocation[] frameLoc)
	{
		Context lower = buildLocals(c, frameLoc[0]);
		
		if (lower != null)
		{
			LexLocation rootLoc = lower.location;
			lower = buildArguments((RootContext)lower, frameLoc[0]);
			frameLoc[0] = rootLoc;	// update location
		}
		
		return lower;
	}
	
	private RootContext buildLocals(Context c, LexLocation frameLoc)
	{
		Context locals = new Context(frameLoc, "Locals", null);

		while (!(c instanceof RootContext) && c != null)
		{
			locals.putAll(c);
			c = c.outer;
		}

		framesToVarReferences.get(nextFrameId).add(nextVariablesReference);
		variablesReferences.put(nextVariablesReference, locals);
		
		nextVariablesReference++;
		return (RootContext) c;
	}
	
	private Context buildArguments(RootContext c, LexLocation frameLoc)
	{
		Context arguments = new Context(frameLoc, "Arguments", null);
		arguments.putAll(c);

		framesToVarReferences.get(nextFrameId).add(nextVariablesReference);
		variablesReferences.put(nextVariablesReference, arguments);
		
		nextVariablesReference++;
		return c.outer;
	}
}
