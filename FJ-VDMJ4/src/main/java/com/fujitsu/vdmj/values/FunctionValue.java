/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.values;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitFunctionDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.patterns.INPatternListList;
import com.fujitsu.vdmj.in.types.INPatternListTypePair;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ClassContext;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

public class FunctionValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final LexLocation location;
	public final String name;
	public Context typeValues;
	public TCFunctionType type;
	public final INPatternListList paramPatternList;
	public final INExpression body;
	public final FunctionValue precondition;
	public final FunctionValue postcondition;
	public final Context freeVariables;
	public final INClassDefinition classdef;

	// Causes parameter assignments to check their invariants (if any).
	// This is set to false for inv_() functions, which cannot check them.
	private final boolean checkInvariants;

	// Measure function value, if any
	private TCNameToken measureName = null;
	private FunctionValue measure = null;
	private Map<Long, Stack<Value>> measureValues = null;
	private Set<Long> measuringThreads = null;
	private Set<Long> callingThreads = null;
	private ValueList curriedArgs = null;
	private boolean isMeasure = false;

	public ObjectValue self = null;
	public boolean isStatic = false;
	public boolean uninstantiated = false;

	private FunctionValue(LexLocation location, String name, TCFunctionType type,
		INPatternListList paramPatternList, INExpression body,
		FunctionValue precondition, FunctionValue postcondition,
		Context freeVariables, boolean checkInvariants, ValueList curriedArgs,
		TCNameToken measureName, Map<Long, Stack<Value>> measureValues, INClassDefinition classdef)
	{
		this.location = location;
		this.name = name;
		this.typeValues = null;
		this.type = type;
		this.paramPatternList = paramPatternList;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.freeVariables = freeVariables;
		this.checkInvariants = checkInvariants;
		this.curriedArgs = curriedArgs;
		this.classdef = classdef;
		
		if (Settings.measureChecks && measureName != null)
		{
			this.measureName = measureName;
			this.measureValues = measureValues;	// NB. a copy of the base FunctionValue's
		}
	}

	// This is used by lambda expressions - so no classdef
	public FunctionValue(LexLocation location, String name, TCFunctionType type,
		INPatternList paramPatterns, INExpression body, Context freeVariables)
	{
		this.location = location;
		this.name = name;
		this.typeValues = null;
		this.type = type;
		this.paramPatternList = new INPatternListList();
		this.body = body;
		this.precondition = null;
		this.postcondition = null;
		this.freeVariables = freeVariables;
		this.checkInvariants = true;
		this.classdef = null;

		paramPatternList.add(paramPatterns);
	}

	public FunctionValue(INExplicitFunctionDefinition def,
		FunctionValue precondition, FunctionValue postcondition,
		Context freeVariables)
	{
		this.location = def.location;
		this.name = def.name.getName();
		this.typeValues = null;
		this.type = (TCFunctionType)def.getType();
		this.paramPatternList = def.paramPatternList;
		this.body = def.body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.freeVariables = freeVariables;
		this.checkInvariants = !def.isTypeInvariant;
		this.classdef = def.classDefinition;

		if (Settings.measureChecks && def.measure != null)
		{
			measureName = def.measure;
			measureValues = Collections.synchronizedMap(new HashMap<Long, Stack<Value>>());
		}
	}

	public FunctionValue(INImplicitFunctionDefinition def,
		FunctionValue precondition, FunctionValue postcondition,
		Context freeVariables)
	{
		this.location = def.location;
		this.name = def.name.getName();
		this.typeValues = null;
		this.type = (TCFunctionType)def.getType();

		this.paramPatternList = new INPatternListList();
		INPatternList plist = new INPatternList();

		for (INPatternListTypePair ptp: def.parameterPatterns)
		{
			plist.addAll(ptp.patterns);
		}

		this.paramPatternList.add(plist);

		this.body = def.body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.freeVariables = freeVariables;
		this.checkInvariants = true;
		this.classdef = def.classDefinition;

		if (Settings.measureChecks && def.measure != null)
		{
			measureName = def.measure;
			measureValues = Collections.synchronizedMap(new HashMap<Long, Stack<Value>>());
		}
	}

	public FunctionValue(INImplicitFunctionDefinition fdef,
		TCFunctionType ftype, Context argTypes, FunctionValue precondition,
		FunctionValue postcondition, Context freeVariables)
	{
		this(fdef, precondition, postcondition, freeVariables);
		this.typeValues = argTypes;
		this.type = ftype;
	}

	public FunctionValue(INExplicitFunctionDefinition fdef,
		TCFunctionType ftype, Context argTypes, FunctionValue precondition,
		FunctionValue postcondition, Context freeVariables)
	{
		this(fdef, precondition, postcondition, freeVariables);
		this.typeValues = argTypes;
		this.type = ftype;
	}

	// This constructor is used by IterFunctionValue and CompFunctionValue
	// The methods which matter are overridden in those classes.

	public FunctionValue(LexLocation location, TCFunctionType type, String name, INClassDefinition classdef)
	{
		this.location = location;
		this.name = name;
		this.typeValues = null;
		this.type = type;
		this.paramPatternList = null;
		this.body = null;
		this.precondition = null;
		this.postcondition = null;
		this.freeVariables = null;
		this.checkInvariants = true;
		this.classdef = classdef;
	}

	@Override
	public String toString()
	{
		return type.toString();
	}

	public Value eval(
		LexLocation from, Value arg, Context ctxt) throws ValueException
	{
		ValueList args = new ValueList(arg);
		return eval(from, args, ctxt, null);
	}

	public Value eval(
		LexLocation from, ValueList argValues, Context ctxt) throws ValueException
	{
		return eval(from, argValues, ctxt, null);
	}

	public void setSelf(ObjectValue self)
	{
		if (!isStatic)
		{
			this.self = self;

			if (measure != null)
			{
				measure.setSelf(self);
			}
		}
	}

	public Value eval(
		LexLocation from, ValueList argValues, Context ctxt, Context sctxt) throws ValueException
	{
		if (body == null)
		{
			abort(4051, "Cannot apply implicit function: " + name, ctxt);
		}

		if (uninstantiated)
		{
			abort(3033, "Polymorphic function has not been instantiated: " + name, ctxt);
		}

		INPatternList paramPatterns = paramPatternList.get(0);
		RootContext evalContext = newContext(from, toTitle(), ctxt, sctxt);

		if (typeValues != null)
		{
			// Add any @T type values, for recursive polymorphic functions
			evalContext.putAll(typeValues);
		}

		if (argValues.size() != paramPatterns.size())
		{
			ExceptionHandler.abort(type.location, 4052, "Wrong number of arguments passed to " + name, ctxt);
		}

		Iterator<Value> valIter = argValues.iterator();
		Iterator<TCType> typeIter = type.parameters.iterator();
		NameValuePairMap args = new NameValuePairMap();

		for (INPattern p: paramPatterns)
		{
			Value pv = valIter.next();

			if (checkInvariants)	// Don't even convert invariant arg values
			{
				pv = pv.convertTo(typeIter.next(), ctxt);
			}

			try
			{
				for (NameValuePair nvp: p.getNamedValues(pv, ctxt))
				{
					Value v = args.get(nvp.name);

					if (v == null)
					{
						args.put(nvp);
					}
					else	// Names match, so values must also
					{
						if (!v.equals(nvp.value))
						{
							abort(4053, "Parameter patterns do not match arguments", ctxt);
						}
					}
				}
			}
			catch (PatternMatchException e)
			{
				abort(e.number, e, ctxt);
			}
		}

		if (self != null)
		{
			evalContext.put(new TCNameToken(location, location.module, "self"), self);
		}

		evalContext.putAll(args);

		if (paramPatternList.size() == 1)
		{
			if (precondition != null && Settings.prechecks)
			{
				// Evaluate pre/post in evalContext as it includes the type
				// variables, if any. We disable the swapping and time (RT)
				// as precondition checks should be "free".

				try
				{
					evalContext.threadState.setAtomic(true);
					evalContext.setPrepost(4055, "Precondition failure: ");
					precondition.eval(from, argValues, evalContext);
				}
				finally
				{
					evalContext.setPrepost(0, null);
					evalContext.threadState.setAtomic(false);
				}
			}

			Long tid = Thread.currentThread().getId();

			if (isMeasure)
			{
				if (measuringThreads.contains(tid))		// We are measuring on this thread
				{
    				if (!callingThreads.add(tid))		// And we've been here already
    				{
    					abort(4148, "Measure function is called recursively: " + name, evalContext);
    				}
				}
			}

			if (measureName != null)
			{
				if (measure == null)
				{
					measure = evalContext.lookup(measureName).functionValue(ctxt);

					if (typeValues != null)		// Function is polymorphic, so measure copies type args
					{
						measure = (FunctionValue)measure.clone();
						measure.uninstantiated = false;
						measure.typeValues = typeValues;
					}

					measure.measuringThreads = Collections.synchronizedSet(new HashSet<Long>());
					measure.callingThreads = Collections.synchronizedSet(new HashSet<Long>());
					measure.isMeasure = true;
				}
				
				// If this is a curried function, then the measure is called with all of the
				// previously applied argument values, in addition to the argValues.
				
				ValueList measureArgs = null;
				
				if (curriedArgs == null)
				{
					measureArgs = argValues;
				}
				else
				{
					measureArgs = new ValueList();
					measureArgs.addAll(curriedArgs);	// Previous args
					measureArgs.addAll(argValues);		// Final args
				}

				// We disable the swapping and time (RT) as measure checks should be "free".
				Value mv;
				
				try
				{
					measure.measuringThreads.add(tid);
					evalContext.threadState.setAtomic(true);
					mv = measure.eval(measure.location, measureArgs, evalContext).deref();
				}
				finally
				{
					evalContext.threadState.setAtomic(false);
					measure.measuringThreads.remove(tid);
				}

				Stack<Value> stack = measureValues.get(tid);

				if (stack == null)
				{
					stack = new Stack<Value>();
					measureValues.put(tid, stack);
				}

				if (!stack.isEmpty())
				{
					Value old = stack.peek();		// Previous value

    				if (old != null && mv.compareTo(old) >= 0)		// Not decreasing order
    				{
    					String message = "Measure failure: " +
    						name + Utils.listToString("(", argValues, ", ", ")") + ", measure " +
    						measure.name + ", current " + mv + ", previous " + old;
    					
    					measure = null;	// Re-initialise counters
    					abort(4146, message, evalContext);
    				}
				}

				stack.push(mv);
			}

			Value rv = null;
			
			try
			{
				// Functions are executed atomically, so they can safely access arg object state
				evalContext.threadState.setAtomic(true);
				evalContext.threadState.setPure(true);
				rv = body.eval(evalContext).convertTo(type.result, evalContext);
			}
			finally
			{
				evalContext.threadState.setAtomic(false);
				evalContext.threadState.setPure(false);
			}

    		if (ctxt.prepost > 0)	// Note, caller's context is checked
    		{
    			if (!rv.boolValue(ctxt))
    			{
    				// Note that this calls getLocation to find out where the body
    				// wants to report its location for this error - this may be an
    				// errs clause in some circumstances.

    				ExceptionHandler.handle(new ContextException(ctxt.prepost,
    						ctxt.prepostMsg + name, body.getLocation(), evalContext));
    			}
    		}

			if (postcondition != null && Settings.postchecks)
			{
				ValueList postArgs = new ValueList(argValues);
				postArgs.add(rv);

				// Evaluate pre/post in evalContext as it includes the type
				// variables, if any. We disable the swapping and time (RT)
				// as postcondition checks should be "free".

				try
				{
					evalContext.threadState.setAtomic(true);
					evalContext.setPrepost(4056, "Postcondition failure: ");
					postcondition.eval(from, postArgs, evalContext);
				}
				finally
				{
					evalContext.setPrepost(0, null);
					evalContext.threadState.setAtomic(false);
				}
			}

			if (measure != null)
			{
				measureValues.get(tid).pop();
			}

			if (isMeasure)
			{
				callingThreads.remove(tid);
			}

			return rv;
		}
		else	// This is a curried function
		{
			if (type.result instanceof TCFunctionType)
			{
				// If a curried function has a pre/postcondition, then the
				// result of a partial application has a pre/post condition
				// with its free variables taken from the environment (so
				// that parameters passed are fixed in subsequent applies).

				FunctionValue newpre = null;

				if (precondition != null)
				{
					newpre = precondition.curry(evalContext);
				}

				FunctionValue newpost = null;

				if (postcondition != null)
				{
					newpost = postcondition.curry(evalContext);
				}

				if (freeVariables != null)
				{
					evalContext.putAll(freeVariables);
				}
				
				// Curried arguments are collected so that we can invoke any measure functions
				// once we reach the final apply that does not return a function.
				
				ValueList argList = new ValueList();
				
				if (curriedArgs != null)
				{
					argList.addAll(curriedArgs);
				}
				
				argList.addAll(argValues);

    			FunctionValue rv = new FunctionValue(location, "curried",
    				(TCFunctionType)type.result,
    				paramPatternList.subList(1, paramPatternList.size()),
    				body, newpre, newpost, evalContext, false, argList,
    				measureName, measureValues, classdef);

    			rv.setSelf(self);
    			rv.typeValues = typeValues;

        		return rv;
			}

			ExceptionHandler.abort(type.location, 4057, "Curried function return type is not a function", ctxt);
			return null;
		}
	}

	private RootContext newContext(LexLocation from, String title, Context ctxt, Context sctxt)
	{
		RootContext evalContext;

		if (self != null)
		{
			evalContext = new ObjectContext(
				from, title, freeVariables, ctxt, self);
		}
		else if (classdef != null)
		{
			evalContext = new ClassContext(
				from, title, freeVariables, ctxt, classdef);
		}
		else
		{
			evalContext = new StateContext(
				from, title, freeVariables, ctxt, sctxt);
		}

		return evalContext;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

			if (val instanceof CompFunctionValue || val instanceof IterFunctionValue)
			{
				return false;	// Play safe - we can't really tell
			}
			else if (val instanceof FunctionValue)
    		{
    			FunctionValue ov = (FunctionValue)val;
    			return ov.type.equals(type) &&		// Param and result types same
    				   ov.body.equals(body);		// Not ideal - a string comparison in fact
    		}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return type.hashCode() + body.hashCode();
	}

	@Override
	public String kind()
	{
		return "function";
	}

	@Override
	public FunctionValue functionValue(Context ctxt)
	{
		return this;
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to.isFunction(location))
		{
			if (type.equals(to) || to.isUnknown(location))
			{
				return this;
			}
			else
			{
				TCFunctionType restrictedType = to.getFunction();
				
				if (type.equals(restrictedType))
				{
					return this;
				}
				else
				{
					if (type.parameters.size() != restrictedType.parameters.size())
					{
						abort(4171, "Cannot convert " + type + " to " + to, ctxt);
					}
					
					TCTypeList domain = TypeComparator.narrowest(type.parameters, restrictedType.parameters);
					TCType range = TypeComparator.narrowest(type.result, restrictedType.result);
					TCFunctionType newType = new TCFunctionType(location, domain, true, range);

					// Create a new function with restricted dom/rng
					FunctionValue restricted = new FunctionValue(location, name, newType,
							paramPatternList, body, precondition, postcondition,
							freeVariables, checkInvariants, curriedArgs,
							measureName, measureValues, classdef);

					restricted.typeValues = typeValues;
					return restricted;
				}
			}
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	private FunctionValue curry(Context newFreeVariables)
	{
		// Remove first set of parameters, and set the free variables instead.
		// And adjust the return type to be the result type (a function).

		return new FunctionValue(location, name, (TCFunctionType)type.result,
			paramPatternList.subList(1, paramPatternList.size()),
			body, precondition, postcondition, newFreeVariables, false, null, null, null, classdef);
	}

	@Override
	public Object clone()
	{
		FunctionValue copy = new FunctionValue(location, name, type,
			paramPatternList, body, precondition, postcondition,
			freeVariables, checkInvariants, curriedArgs,
			measureName, measureValues, classdef);

		copy.typeValues = typeValues;
		return copy;
	}

	public String toTitle()
	{
		INPatternList paramPatterns = paramPatternList.get(0);
		return name + Utils.listToString("(", paramPatterns, ", ", ")");
	}
}
