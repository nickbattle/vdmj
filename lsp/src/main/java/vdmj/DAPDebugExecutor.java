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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import com.fujitsu.vdmj.debug.DebugCommand;
import com.fujitsu.vdmj.debug.DebugExecutor;
import com.fujitsu.vdmj.debug.DebugType;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INHistoryExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.ClassContext;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.FieldValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.ReferenceValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;

import json.JSONArray;
import json.JSONObject;
import workspace.Diag;

public class DAPDebugExecutor implements DebugExecutor
{
	private static class Frame
	{
		public Frame(int frameId, LexLocation frameLoc)
		{
			this.frameId = frameId;
			this.location = frameLoc;
		}
		
		public final int frameId;
		public final LexLocation location;
		public String title;
		public List<Scope> scopes = new Vector<Scope>();
		public int outerId = 0;
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
	private final LexLocation breakloc;
	/** The context that was active when the thread stopped. */
	private final Context ctxt;
	/** The frameId of the top of the cached ctxt stack */
	private int topFrameId;

	/** Representation of ctxt for DAP responses */
	private static Map<Integer, Frame> ctxtFrames;
	private static AtomicInteger nextFrameId;

	private static Map<Integer, Object> variablesReferences;
	private static AtomicInteger nextVariablesReference;


	public DAPDebugExecutor(LexLocation breakloc, Context ctxt)
	{
		this.interpreter = Interpreter.getInstance();
		this.breakloc = breakloc;
		this.ctxt = ctxt;
		
		buildCache();
	}
	
	public static void init()
	{
		variablesReferences = Collections.synchronizedMap(new LinkedHashMap<Integer, Object>());
		nextVariablesReference = new AtomicInteger();
		nextVariablesReference.set(1000);	// So we can tell them apart (roughly)

		ctxtFrames = Collections.synchronizedMap(new LinkedHashMap<Integer, Frame>());
		nextFrameId = new AtomicInteger();
		nextFrameId.set(100);
	}

	@Override
	public void clear()
	{
		variablesReferences.clear();
		ctxtFrames.clear();
		// Keep "next" values until next init() call
	}
	
	/**
	 * Perform one debugger command
	 */
	@Override
	public DebugCommand run(DebugCommand request)
	{
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
					
				case SCOPES:
					result = doScopes(request);
					break;
					
				case VARIABLES:
					result = doVariables(request);
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
		JSONObject arguments = (JSONObject) command.getPayload();
		String expr = arguments.get("expression");
		String error = null;
		Value answer = null;
		String savedName = interpreter.getDefaultName();

		try
		{
			interpreter.setDefaultName(breakloc.module);
			ctxt.threadState.setAtomic(true);
			answer = interpreter.evaluate(expr, ctxt);
 		}
		catch (ParserException e)
		{
			error = simplify(e.getMessage());
		}
		catch (ContextException e)
		{
			error = simplify(e.getMessage());
		}
		catch (RuntimeException e)
		{
			error = "Runtime: " + e.getMessage();
		}
		catch (Exception e)
		{
			while (e instanceof InvocationTargetException)
			{
				e = (Exception)e.getCause();
			}
			
			error = "Error: " + e.getMessage();
		}
		catch (Throwable th)
		{
			error = "Error: " + th.getMessage();
		}
		finally
		{
			try
			{
				interpreter.setDefaultName(savedName);
			}
			catch (Exception e)
			{
				Diag.error(e);
			}

			ctxt.threadState.setAtomic(false);
		}

		if (error != null)
		{
			return new DebugCommand(DebugType.PRINT, new JSONObject("result", error, "variablesReference", 0));
		}
		else
		{
			return new DebugCommand(DebugType.PRINT, new JSONObject(
					"result", answer.toString(), "variablesReference", valueToReference(answer)));
		}
	}

	private String simplify(String message)
	{
		if (message.startsWith("Error 4034:"))	// Not in scope
		{
			return "not available";		// Default text in VSC watches
		}
		else
		{
			return message;
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
		Long startFrame = arguments.get("startFrame", 0L);
		Long levels = arguments.get("levels", 0L);
		
		JSONArray frames = new JSONArray();
		int frameId = topFrameId;
		int totalFrames = 0;
		
		while (frameId != 0)
		{
			Frame frame = ctxtFrames.get(frameId);
			
			if (frame != null)	// vscode bug? Sometimes sends late? request for invalid frameId
			{
				if (totalFrames >= startFrame && (frames.size() < levels || levels == 0))
				{
					frames.add(new JSONObject(
							"id",		frame.frameId,
							"name",		frame.title,
							"source",	locationToSource(frame.location),
							"line",		frame.location.startLine,
							"column",	frame.location.startPos,
							"moduleId",	frame.location.module));
				}
				
				totalFrames++;
				frameId = frame.outerId;
			}
			else
			{
				Diag.error("Invalid frameId in stack request: %d", frameId);
				frameId = 0;
			}
		}
		
		JSONObject stackResponse = new JSONObject("stackFrames", frames, "totalFrames", totalFrames);
		return new DebugCommand(DebugType.STACK, stackResponse);
	}
	
	private JSONObject locationToSource(LexLocation location)
	{
		if (location.file.getName().equals("?") ||
			location.file.getName().equals("console"))
		{
			return new JSONObject(
				"name", location.file.getName(),
				"origin", "Debug Console",
				"sourceReference", 0);		// See SourceHandler
		}
		else
		{
			return new JSONObject("path", location.file.getAbsolutePath());
		}
	}
	
	private DebugCommand doScopes(DebugCommand command)
	{
		JSONObject arguments = (JSONObject) command.getPayload();
		long frameId = arguments.get("frameId");
		Frame frame = ctxtFrames.get((int)frameId);
		JSONArray scopes = new JSONArray();
		
		if (frame != null)	// vscode bug? Sometimes sends request for invalid frameId
		{
			for (Scope scope: frame.scopes)
			{
				scopes.add(new JSONObject(
					"name", scope.name,
					"variablesReference", scope.vref,
					"source", locationToSource(frame.location)
				));
			}
		}
		else
		{
			Diag.error("Invalid frameId in scopes request: %d", frameId);
			// return an empty scopes array
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
		
		if (var instanceof ReferenceValue)
		{
			var = ((ReferenceValue)var).deref();
		}
		
		if (var instanceof Context)
		{
			Context c = (Context)var;
			TCNameList sorted = new TCNameList();
			sorted.addAll(c.keySet());
			Collections.sort(sorted);
			
			for (TCNameToken name: sorted)
			{
				Value value = c.get(name);
				
				variables.add(new JSONObject(
					"name", name.toString(),
					"value", value.toString(),
					"variablesReference", valueToReference(value))
				);
			}
		}
		else if (var instanceof Map)	// eg, "inherited", arbitrary string pairs
		{
			@SuppressWarnings("unchecked")
			Map<String, String> map = (Map<String, String>)var;
			List<String> sorted = new Vector<String>();
			sorted.addAll(map.keySet());
			Collections.sort(sorted);
			
			for (String name: sorted)
			{
				String value = map.get(name);
				
				variables.add(new JSONObject(
					"name", name,
					"value", value,
					"variablesReference", 0L));
			}		
		}
		else if (var instanceof RecordValue)
		{
			RecordValue r = (RecordValue)var;
			
			for (FieldValue field: r.fieldmap)
			{
				variables.add(new JSONObject(
					"name", field.name,
					"value", field.value.toString(),
					"variablesReference", valueToReference(field.value))
				);
			}
		}
		else if (var instanceof SetValue)
		{
			SetValue s = (SetValue)var;
			int i = 1;
			
			for (Value value: s.values)
			{
				variables.add(new JSONObject(
						"name", "{" + i++ + "}",
						"value", value.toString(),
						"variablesReference", valueToReference(value))
					);
			}
		}
		else if (var instanceof SeqValue)
		{
			SeqValue s = (SeqValue)var;
			int i = 1;
			
			for (Value value: s.values)
			{
				variables.add(new JSONObject(
						"name", "[" + i++ + "]",
						"value", value.toString(),
						"variablesReference", valueToReference(value))
					);
			}
		}
		else if (var instanceof ObjectValue)
		{
			ObjectValue obj = (ObjectValue)var;
			String className = obj.type.name.getName();
			NameValuePairMap all = obj.getMemberValues();
			TCNameList sorted = new TCNameList();
			sorted.addAll(all.keySet());
			Collections.sort(sorted);

			Map<String, String> inherited = new HashMap<String, String>();
			JSONArray members = new JSONArray();
			
			for (TCNameToken name: sorted)
			{
				Value value = all.get(name);
				
				if (value instanceof FunctionValue ||
					value instanceof OperationValue)
				{
					continue;	// skip func/op members
				}
				
				if (name.getModule().equals(className))
				{
					members.add(new JSONObject(
							"name", name.toString(),
							"value", value.toString(),
							"variablesReference", valueToReference(value))
						);
				}
				else
				{
					inherited.put(name.getExplicit(true).toString(), value.toString());
				}
			}
			
			if (!inherited.isEmpty())
			{
				// Result has inherited first...
				variables.add(new JSONObject(
						"name", "inherited",
						"value", "",
						"variablesReference", valueToReference(inherited)));
			}
			
			variables.addAll(members);
		}
		else if (var instanceof MapValue)
		{
			MapValue m = (MapValue)var;
			
			for (Value key: m.values.keySet())
			{
				variables.add(new JSONObject(
					"name", key.toString(),
					"value", m.values.get(key).toString(),
					"variablesReference", valueToReference(m.values.get(key)))
				);
			}
		}
		else if (var instanceof TupleValue)
		{
			TupleValue t = (TupleValue)var;
			int i = 1;
			
			for (Value value: t.values)
			{
				variables.add(new JSONObject(
						"name", "#" + i++,
						"value", value.toString(),
						"variablesReference", valueToReference(value))
					);
			}

		}
		
		return variables;
	}

	private Long valueToReference(Object value)
	{
		if (value instanceof ReferenceValue)
		{
			value = ((ReferenceValue)value).deref();
		}

		if (value instanceof RecordValue ||
			value instanceof SetValue ||
			value instanceof SeqValue ||
			value instanceof ObjectValue ||
			value instanceof MapValue ||
			value instanceof TupleValue ||
			value instanceof Map)
		{
			int ref = nextVariablesReference.incrementAndGet();
			variablesReferences.put(ref, value);
			return (long) ref;
		}
		
		return 0L;	// ie. no reference
	}

	private DebugCommand doStop()
	{
		return DebugCommand.STOP;
	}

	private DebugCommand doQuit()
	{
		return DebugCommand.QUIT;
	}
	
	/**
	 * Build the ctxtFrames and scopes for the stack. VDMJ stacks contain Contexts for
	 * local "let" variables etc, and RootContexts that contain any arguments. The
	 * initial Frame is located at the break/stop location. Subsequent Frames are located
	 * where that is called from, hence the nextLoc being passed back.
	 */
	private void buildCache()
	{
		LexLocation[] nextLoc = { breakloc };
		Frame prevFrame = null;
		Context c = ctxt;
		
		while (c != null)
		{
			int frameId = nextFrameId.incrementAndGet();
			
			if (prevFrame == null)
			{
				topFrameId = frameId;
			}
			else
			{
				prevFrame.outerId = frameId;
			}
			
			Frame frame = new Frame(frameId, nextLoc[0]);
			ctxtFrames.put(frameId, frame);
			prevFrame = frame;

			c = buildScopes(c, frame, nextLoc);
		}
	}
	
	/**
	 * Build Scopes for any guards, any local variables and any arguments passed.
	 * The buildLocals and buildArguments return the next Context down the stack.
	 * The location for the next frame is set to the LexLocation of the argument
	 * frame (the RootContext), which is where this fn/op was called from. The
	 * locations are "out by one" because the first frame location is the breakpoint.
	 * See above. 
	 */
	private Context buildScopes(Context c, Frame frame, LexLocation[] nextLoc)
	{
		if (c.guardOp != null)
		{
			buildGuards(c, frame);
		}
		
		RootContext arguments = buildLocals(c, frame);
		
		if (arguments != null)
		{
			Context nextFrame = buildArguments(arguments, frame);
			nextLoc[0] = arguments.location;
			return nextFrame;
		}
		
		return null;	// No further frames
	}
	
	/**
	 * Add any guards to the frame in a named scope
	 */
	private void buildGuards(Context c, Frame frame)
	{
		if (c instanceof ObjectContext)
		{
			Context guards = new Context(frame.location, "Guards", null);
			ObjectContext octxt = (ObjectContext)c;
			int line = breakloc.startLine;
			String opname = c.guardOp.name.getName();

			for (TCDefinition d: octxt.self.type.classdef.definitions)
			{
				if (d instanceof TCPerSyncDefinition)
				{
					try
					{
						TCPerSyncDefinition pdef = (TCPerSyncDefinition)d;
						INExpression guard = ClassMapper.getInstance(INNode.MAPPINGS).convertLocal(pdef.guard);

						if (pdef.opname.getName().equals(opname) ||
							pdef.location.startLine == line ||
							guard.findExpression(line) != null)
						{
							for (INExpression sub: guard.getHistoryExpressions())
							{
								INHistoryExpression hexp = (INHistoryExpression)sub;
								Value v = hexp.eval(octxt);
								TCNameToken name =
									new TCNameToken(pdef.location, octxt.self.type.name.getModule(),
										hexp.toString());
								guards.put(name, v);
							}
						}
					}
					catch (Exception e)
					{
						Diag.error(e);
					}
				}
				else if (d instanceof TCMutexSyncDefinition)
				{
					TCMutexSyncDefinition mdef = (TCMutexSyncDefinition)d;

    				for (TCNameToken mop: mdef.operations)
    				{
    					if (mop.getName().equals(opname))
    					{
            				for (TCNameToken op: mdef.operations)
            				{
            					TCNameList ops = new TCNameList(op);
            					INHistoryExpression hexp = new INHistoryExpression(mdef.location, Token.ACTIVE, ops);
            					Value v = hexp.eval(octxt);
        						TCNameToken name =
        							new TCNameToken(mdef.location, octxt.self.type.name.getModule(),
        								hexp.toString());
        						guards.put(name, v);
            				}

            				break;
    					}
    				}
				}
			}
			
			if (!guards.isEmpty())
			{
				int vref = nextVariablesReference.incrementAndGet();
				variablesReferences.put(vref, guards);
				frame.scopes.add(new Scope("Guards", vref));
			}
		}
	}

	/**
	 * Add a "Locals" scope for "let" contexts that are not arguments in a RootContext.
	 * Return the RootContext for argument processing, or null at the outer level.
	 */
	private RootContext buildLocals(Context c, Frame frame)
	{
		Context locals = new Context(frame.location, "Locals", null);
		List<Context> lets = new Vector<Context>();

		while (!(c instanceof RootContext) && c != null)
		{
			lets.add(0, c);		// Keep top level names last, to show correct "hiding"
			c = c.outer;
		}
		
		for (Context let: lets)
		{
			locals.putAll(let);
		}

		if (!locals.isEmpty())
		{
			int vref = nextVariablesReference.incrementAndGet();
			variablesReferences.put(vref, locals);
			frame.scopes.add(new Scope("Locals", vref));
		}
		
		return (RootContext) c;
	}
	
	/**
	 * Add a scope for any RootContext variables (arguments or globals).
	 * Return the next Context or null at the outer level.
	 */
	private Context buildArguments(RootContext c, Frame frame)
	{
		String scope = (c.outer == null ? "Globals" : "Arguments");
		Context arguments = new Context(c.location, scope, null);
		
		for (Entry<TCNameToken, Value> nvp: c.entrySet())
		{
			// We eliminate operations from the context, since those are not valuable in
			// a stack frame. But functions can be (eg. constant lambdas, comps or iterations).
			// So we have to distinguish function definitions using the "name" of the value.
			
			if (nvp.getValue() instanceof OperationValue)
			{
				continue;
			}
			
			if (nvp.getValue() instanceof FunctionValue)
			{
				FunctionValue fv = (FunctionValue)nvp.getValue();
				
				if (!fv.name.equals("lambda") &&
					!fv.name.equals("comp") &&
					!fv.name.equals("**"))
				{
					continue;
				}
			}

			arguments.put(nvp.getKey(), nvp.getValue());
		}
		
		if (c instanceof StateContext)
		{
			StateContext s = (StateContext)c;
			
			if (s.stateCtxt != null)	// module's state variables
			{
				int vref = nextVariablesReference.incrementAndGet();
				variablesReferences.put(vref, s.stateCtxt);
				frame.scopes.add(new Scope("State", vref));
			}
		}
		else if (c instanceof ClassContext)
		{
			ClassContext s = (ClassContext)c;
			Context statics = s.classdef.getStatics();
		    Iterator<Map.Entry<TCNameToken, Value>> it = statics.entrySet().iterator();
		    
	        while (it.hasNext())
	        {
	        	Entry<TCNameToken, Value> e = it.next();
	        	
				if (e.getValue() instanceof FunctionValue || e.getValue() instanceof OperationValue)
				{
	                it.remove();
	            }
	        }
			
			if (!statics.isEmpty())
			{
				int vref = nextVariablesReference.incrementAndGet();
				variablesReferences.put(vref, statics);
				frame.scopes.add(new Scope("Statics", vref));
			}
		}

		if (!arguments.isEmpty())
		{
			int vref = nextVariablesReference.incrementAndGet();
			variablesReferences.put(vref, arguments);
			frame.scopes.add(new Scope(scope, vref));
		}

		frame.title = c.title;	// Title comes from RootContext
		return c.outer;
	}
	
//	private LexLocation locationFromCtxt(Context c)
//	{
//		LexLocation loc = c.location;
//		
//		for (Entry<TCNameToken, Value> entry: c.entrySet())
//		{
//			if (entry.getValue() instanceof OperationValue)
//			{
//				OperationValue op = (OperationValue)entry.getValue();
//				loc = op.name.getLocation();
//				loc = new LexLocation(loc.file, "DEFAULT", 0, 0, 0, 0);
//				break;
//			}
//			else if (entry.getValue() instanceof FunctionValue)
//			{
//				FunctionValue fn = (FunctionValue)entry.getValue();
//				loc = fn.location;
//				loc = new LexLocation(loc.file, "DEFAULT", 0, 0, 0, 0);
//				break;
//			}
//		}
//		
//		return loc;
//	}
}
