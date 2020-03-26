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
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
import com.fujitsu.vdmj.values.FieldValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;

import json.JSONArray;
import json.JSONObject;
import workspace.Log;

public class DAPDebugExecutor implements DebugExecutor
{
	private static class Frame
	{
		public Frame(int frameId, LexLocation frameLoc)
		{
			this.frameId = frameId;
			this.location = frameLoc;
		}
		
		public int frameId;
		public LexLocation location;
		public String title;
		public List<Scope> scopes = new Vector<Scope>();
	}
	
	private static class Scope
	{
		public Scope(String name, int vref)
		{
			this.name = name;
			this.vref = vref;
		}
		
		public String name;
		public int vref;
	}
	
	/** The interpreter */
	private final Interpreter interpreter;

	/** The location where the thread stopped. */
	private LexLocation breakloc;
	/** The context that was active when the thread stopped. */
	private Context ctxt;

	/** Representation of ctxt for DAP responses */
	private Map<Integer, Frame> ctxtFrames;
	private Map<Integer, Object> variablesReferences;
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
		
		rebuildCache();
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
   		return DebugCommand.RESUME;		// null JSON body is ok
	}

	private DebugCommand doNext()
	{
		ctxt.threadState.setBreaks(breakloc, ctxt.getRoot(), null);
   		return DebugCommand.RESUME;		// null JSON body is ok
	}

	private DebugCommand doOut()
	{
		ctxt.threadState.setBreaks(breakloc, null, ctxt.getRoot().outer);
   		return DebugCommand.RESUME;		// null JSON body is ok
	}

	private DebugCommand doContinue()
	{
		ctxt.threadState.setBreaks(null, null, null);
   		return new DebugCommand(DebugType.RESUME, new JSONObject("allThreadsContinued", true));
	}

	private DebugCommand doStack(DebugCommand command)
	{
		JSONObject arguments = (JSONObject) command.getPayload();
		long startFrame = arguments.get("startFrame");
		long levels = arguments.get("levels");
		
		int totalFrames = ctxtFrames.size();
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
				Frame frame = ctxtFrames.get(frameId);
				
				frames.add(new JSONObject(
						"id",		frame.frameId,
						"name",		frame.title,
						"source",	new JSONObject("path", frame.location.file.getAbsolutePath()),
						"line",		frame.location.startLine,
						"column",	frame.location.startPos,
						"moduleId",	frame.location.module));
				
				if (frames.size() >= levels) break;
			}
			
			stackResponse = new JSONObject("stackFrames", frames, "totalFrames", totalFrames);
		}
		
		return new DebugCommand(DebugType.STACK, stackResponse);
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
		Frame frame = ctxtFrames.get((int)frameId);
		JSONArray scopes = new JSONArray();
		
		for (Scope scope: frame.scopes)
		{
			scopes.add(new JSONObject(
				"name", scope.name,
				"variablesReference", scope.vref,
				"source", new JSONObject("path", frame.location.file.getAbsolutePath())
			));
		}
		
		return new DebugCommand(DebugType.STACK, new JSONObject("scopes", scopes));
	}

	private DebugCommand doVariables(DebugCommand command)
	{
		JSONObject arguments = (JSONObject) command.getPayload();
		long vref = arguments.get("variablesReference");
		Object var = variablesReferences.get((int)vref);
		
		return new DebugCommand(DebugType.STACK, new JSONObject("variables", referenceToVariables(var)));
	}
	
	private JSONArray referenceToVariables(Object var)
	{
		JSONArray variables = new JSONArray();
		
		if (var instanceof Context)
		{
			Context c = (Context)var;
			
			for (TCNameToken name: c.keySet())
			{
				variables.add(new JSONObject(
					"name", name.toString(),
					"value", c.get(name).toString())
				);
			}
		}
		else if (var instanceof RecordValue)
		{
			RecordValue r = (RecordValue)var;
			
			for (FieldValue field: r.fieldmap)
			{
				variables.add(new JSONObject(
					"name", field.name,
					"value", field.value.toString())
				);
			}
		}
		else if (var instanceof SetValue)
		{
			SetValue s = (SetValue)var;
			
			for (Value value: s.values)
			{
				variables.add(new JSONObject(
						"name", "",
						"value", value.toString())
					);
			}
		}
		else if (var instanceof SeqValue)
		{
			SeqValue s = (SeqValue)var;
			
			for (Value value: s.values)
			{
				variables.add(new JSONObject(
						"name", "",
						"value", value.toString())
					);
			}
		}
		
		return variables;
	}

	private DebugCommand doStop()
	{
		return DebugCommand.STOP;
	}

	private DebugCommand doQuit()
	{
		return DebugCommand.QUIT;
	}
	
	private void rebuildCache()
	{
		ctxtFrames = new HashMap<Integer, Frame>();
		variablesReferences = new HashMap<Integer, Object>();
		
		nextFrameId = 0;				// Zero-relative for doStack
		nextVariablesReference = 1;		// Zero reserved?
		
		Context c = ctxt;
		LexLocation[] nextLoc = { breakloc };
		
		while (c != null)
		{
			Frame frame = new Frame(nextFrameId, nextLoc[0]);
			ctxtFrames.put(nextFrameId, frame);
			c = buildScopes(c, frame, nextLoc);
			nextFrameId++;
		}
		
		// Dump to diags...
		for (Integer frameId: ctxtFrames.keySet())
		{
			Frame frame = ctxtFrames.get(frameId);
			Log.printf("======== Frame %s = %s:", frameId, frame.title);
			
			for (Scope scope: frame.scopes)
			{
				Log.printf("-------- Scope %s, vref %d:", scope.name, scope.vref);
				c = (Context) variablesReferences.get(scope.vref);
				
				for (TCNameToken name: c.keySet())
				{
					Value value = c.get(name);
					Log.printf("%s = %s", name, value);
				}
			}
		}
	}
	
	private Context buildScopes(Context c, Frame frame, LexLocation[] nextLoc)
	{
		Context arguments = buildLocals(c, frame);
		
		if (arguments != null)
		{
			nextLoc[0] = arguments.location;
			arguments = buildArguments((RootContext)arguments, frame);
		}
		
		return arguments;
	}
	
	private RootContext buildLocals(Context c, Frame frame)
	{
		Context locals = new Context(frame.location, "Locals", null);

		while (!(c instanceof RootContext) && c != null)
		{
			locals.putAll(c);
			c = c.outer;
		}

		if (!locals.isEmpty())
		{
			variablesReferences.put(nextVariablesReference, locals);
			frame.scopes.add(new Scope("Locals", nextVariablesReference));
			nextVariablesReference++;
		}
		
		return (RootContext) c;
	}
	
	private Context buildArguments(RootContext c, Frame frame)
	{
		String title = (c.outer == null ? "Globals" : "Arguments");
		LexLocation loc = (c.outer == null ? c.location : frame.location);
		Context arguments = new Context(loc, title, null);
		arguments.putAll(c);

		variablesReferences.put(nextVariablesReference, arguments);
		frame.title = c.title;
		frame.scopes.add(new Scope(title, nextVariablesReference));
		nextVariablesReference++;

		return c.outer;
	}
}
