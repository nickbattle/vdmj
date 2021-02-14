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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.values;

import java.util.Iterator;
import java.util.ListIterator;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INStateDefinition;
import com.fujitsu.vdmj.in.definitions.INSystemDefinition;
import com.fujitsu.vdmj.in.expressions.INAndExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.types.INPatternListTypePair;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.runtime.ClassContext;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.StateContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.scheduler.AsyncThread;
import com.fujitsu.vdmj.scheduler.Holder;
import com.fujitsu.vdmj.scheduler.Lock;
import com.fujitsu.vdmj.scheduler.MessageRequest;
import com.fujitsu.vdmj.scheduler.MessageResponse;
import com.fujitsu.vdmj.scheduler.ResourceScheduler;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;


public class OperationValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final INExplicitOperationDefinition expldef;
	public final INImplicitOperationDefinition impldef;
	public final TCNameToken name;
	public final TCOperationType type;
	public final INPatternList paramPatterns;
	public final INStatement body;
	public final FunctionValue precondition;
	public final FunctionValue postcondition;
	public final INStateDefinition state;
	public final INClassDefinition classdef;

	private TCNameToken stateName = null;
	private Context stateContext = null;
	private ObjectValue self = null;

	public boolean isConstructor = false;
	public boolean isStatic = false;
	public boolean isAsync = false;

	private INExpression guard = null;

	public int hashAct = 0; // Number of activations
	public int hashFin = 0; // Number of finishes
	public int hashReq = 0; // Number of requests

	private long priority = 0;
	private boolean traceRT = true;

	public OperationValue(INExplicitOperationDefinition def,
		FunctionValue precondition, FunctionValue postcondition,
		INStateDefinition state)
	{
		this.expldef = def;
		this.impldef = null;
		this.name = def.name;
		this.type = (TCOperationType)def.getType();
		this.paramPatterns = def.parameterPatterns;
		this.body = def.body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.state = state;
		this.classdef = def.classDefinition;
		this.isAsync = def.accessSpecifier.isAsync;

		traceRT =
			Settings.dialect == Dialect.VDM_RT &&
			classdef != null &&
			!(classdef instanceof INSystemDefinition) &&
			!classdef.name.getName().equals("CPU") &&
			!classdef.name.getName().equals("BUS") &&
			!name.getName().equals("thread") &&
			!name.getName().startsWith("inv_");
	}

	public OperationValue(INImplicitOperationDefinition def,
		FunctionValue precondition, FunctionValue postcondition,
		INStateDefinition state)
	{
		this.impldef = def;
		this.expldef = null;
		this.name = def.name;
		this.type = (TCOperationType)def.getType();
		this.paramPatterns = new INPatternList();

		for (INPatternListTypePair ptp : def.parameterPatterns)
		{
			paramPatterns.addAll(ptp.patterns);
		}

		this.body = def.body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.state = state;
		this.classdef = def.classDefinition;
		this.isAsync = def.accessSpecifier.isAsync;

		traceRT =
			Settings.dialect == Dialect.VDM_RT &&
			classdef != null &&
			!(classdef instanceof INSystemDefinition) &&
			!classdef.name.getName().equals("CPU") &&
			!classdef.name.getName().equals("BUS") &&
			!name.getName().equals("thread");
	}

	@Override
	public String toString()
	{
		return type.toString();
	}

	public void setSelf(ObjectValue self)
	{
		if (!isStatic)
		{
			this.self = self;
		}
	}

	public ObjectValue getSelf()
	{
		return self;
	}

	public void setGuard(INExpression add, boolean isMutex)
	{
		if (guard == null)
		{
			guard = add;
		}
		else
		{
			// Create "old and new" expression

			LexLocation where = isMutex ? guard.location : add.location;

			guard = new INAndExpression(guard,
				new LexKeywordToken(Token.AND, where), add);
		}
	}

	public void prepareGuard(ObjectContext ctxt)
	{
		if (guard != null)
		{
			ValueListener vl = new GuardValueListener(getGuardLock());

			for (Value v: guard.getValues(ctxt))
			{
				UpdatableValue uv = (UpdatableValue)v;
				uv.addListener(vl);
			}
		}
	}

	public Value eval(LexLocation from, ValueList argValues, Context ctxt)
		throws ValueException
	{
		try
		{
			// Note args cannot be Updateable, so we convert them here. This means
			// that TransactionValues pass the local "new" value to the far end.
			ValueList constValues = argValues.getConstant();

			if (Settings.dialect == Dialect.VDM_RT)
			{
				if (!isStatic && (ctxt.threadState.CPU != self.getCPU() || isAsync))
				{
					return asyncEval(constValues, ctxt);
				}
				else
				{
					return localEval(from, constValues, ctxt, true);
				}
			}
			else
			{
				return localEval(from, constValues, ctxt, true);
			}
		}
		catch (StackOverflowError e)
		{
			throw new ContextException(4174, "Stack overflow", from, ctxt);
		}
	}

	public Value localEval(
		LexLocation from, ValueList argValues, Context ctxt, boolean logreq)
		throws ValueException
	{
		if (body == null)
		{
			abort(4066, "Cannot call implicit operation: " + name, ctxt);
		}

		if (state != null && stateName == null)
		{
			stateName = state.name;
			stateContext = state.getStateContext();
		}

		RootContext argContext = newContext(from, toTitle(), ctxt);

		req(logreq);
		notifySelf();

		if (guard != null)
		{
			guard(argContext);
		}
		else
		{
			act();		// Still activated, even if no guard
		}

		notifySelf();

		if (argValues.size() != paramPatterns.size())
		{
			abort(4068, "Wrong number of arguments passed to " + name.getName(), ctxt);
		}

		ListIterator<Value> valIter = argValues.listIterator();
		Iterator<TCType> typeIter = type.parameters.iterator();
		NameValuePairMap args = new NameValuePairMap();

		for (INPattern p : paramPatterns)
		{
			try
			{
				// Note values are assumed to be constant, as enforced by eval()
				Value pv = valIter.next().convertTo(typeIter.next(), ctxt);

				for (NameValuePair nvp : p.getNamedValues(pv, ctxt))
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
							abort(4069,	"Parameter patterns do not match arguments", ctxt);
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
			argContext.put(name.getSelfName(), self);
		}

		// Note: arg name/values hide member values
		argContext.putAll(args);

		Value originalSigma = null;
		MapValue originalValues = null;

		if (postcondition != null)
		{
			if (stateName != null)
			{
				Value sigma = argContext.lookup(stateName);
				originalSigma = (Value)sigma.clone();
			}
			else if (self != null)
			{
				// originalSelf = self.shallowCopy();
				TCNameList oldnames = postcondition.body.getOldNames();
				originalValues = self.getOldValues(oldnames);
			}
			else if (classdef != null)
			{
				TCNameList oldnames = postcondition.body.getOldNames();
				originalValues = classdef.getOldValues(oldnames);
			}
		}

		// Make sure the #fin is updated with ErrorExceptions, using the
		// finally clause...

		Value rv = null;

		try
		{
    		if (precondition != null && Settings.prechecks)
    		{
    			ValueList preArgs = new ValueList(argValues);

    			if (stateName != null)
    			{
    				preArgs.add(argContext.lookup(stateName));
    			}
    			else if (self != null)
    			{
    				preArgs.add(self);
    			}

    			// We disable the swapping and time (RT) as precondition checks should be "free".

    			try
    			{
    				ctxt.threadState.setAtomic(true);
    				ctxt.setPrepost(4071, "Precondition failure: ");
    				precondition.eval(from, preArgs, ctxt);
    			}
    			finally
    			{
    				ctxt.setPrepost(0, null);
    				ctxt.threadState.setAtomic(false);
    			}
    		}
    		
    		if (Settings.release == Release.VDM_10 && !type.isPure() && ctxt.threadState.isPure())
    		{
    			abort(4166, "Cannot call impure operation: " + name, ctxt);
    		}

    		rv = body.eval(argContext);

    		if (isConstructor)
    		{
    			rv = self;
    		}
    		else
    		{
    			rv = rv.convertTo(type.result, argContext);
    		}

    		if (postcondition != null && Settings.postchecks)
    		{
    			ValueList postArgs = new ValueList(argValues);

    			if (!(rv instanceof VoidValue))
    			{
    				postArgs.add(rv);
    			}

    			if (stateName != null)
    			{
    				postArgs.add(originalSigma);
    				Value sigma = argContext.lookup(stateName);
    				postArgs.add(sigma);
    			}
    			else if (self != null)
    			{
    				postArgs.add(originalValues);
   					postArgs.add(self);
    			}
    			else if (classdef != null)
    			{
    				postArgs.add(originalValues);
    			}

    			// We disable the swapping and time (RT) as postcondition checks should be "free".

    			try
    			{
    				ctxt.threadState.setAtomic(true);
    				ctxt.setPrepost(4072, "Postcondition failure: ");
    				postcondition.eval(from, postArgs, ctxt);
    			}
    			finally
    			{
    				ctxt.setPrepost(0, null);
    				ctxt.threadState.setAtomic(false);
    			}
    		}
		}
		finally
		{
    		fin();
    		notifySelf();
		}

		return rv;
	}

	private RootContext newContext(LexLocation from, String title, Context ctxt)
	{
		RootContext argContext;

		if (self != null)
		{
			argContext = new ObjectContext(from, title, ctxt, self);
		}
		else if (classdef != null)
		{
			argContext = new ClassContext(from, title, ctxt, classdef);
		}
		else
		{
			argContext = new StateContext(from, title, ctxt, stateContext);
		}

		return argContext;
	}
	
	private Lock getGuardLock()
	{
		if (classdef != null)
		{
			return classdef.guardLock;
		}
		else if (self != null)
		{
			return self.guardLock;
		}
		else
		{
			return null;
		}
	}
	
	private Object getGuardObject(Context ctxt)
	{
		if (ctxt instanceof ClassContext)
		{
			ClassContext cctxt = (ClassContext)ctxt;
			return cctxt.classdef;
		}
		else
		{
			return self;
		}
	}

	private void guard(Context ctxt) throws ValueException
	{
		if (!(Thread.currentThread() instanceof SchedulableThread))
		{
			return;		// Probably during initialization.
		}

		Lock lock = getGuardLock();
		lock.lock(ctxt, guard.location);

		while (true)
		{
			synchronized (getGuardObject(ctxt))		// So that test and act() are atomic
			{
				// We have to suspend thread swapping round the guard,
				// else we will reschedule another CPU thread while
				// having self locked, and that locks up everything!

				try
				{
					debug("guard TEST");
					ctxt.threadState.setAtomic(true);
	    			boolean ok = guard.eval(ctxt).boolValue(ctxt);
	
	    			if (ok)
	    			{
	    				debug("guard OK");
	    				act();
	    				break;	// Out of while loop
	    			}
				}
				finally
				{
	    			ctxt.threadState.setAtomic(false);
				}
			}

			// The guardLock list is signalled by the GuardValueListener
			// and by notifySelf when something changes. The guardOp
			// is set to indicate the guard state to any breakpoints.

			debug("guard WAIT");
			ctxt.guardOp = this;
			lock.block(ctxt, guard.location);
			ctxt.guardOp = null;
			debug("guard WAKE");
		}

		lock.unlock();
	}

	private void notifySelf()
	{
		Lock lock = getGuardLock();
		
		if (lock != null)
		{
			debug("Signal guard");
			lock.signal();
		}
	}

	private Value asyncEval(ValueList argValues, Context ctxt) throws ValueException
	{
		// Spawn a thread, send a message, wait for a reply...

		CPUValue from = ctxt.threadState.CPU;
		CPUValue to = self.getCPU();
		boolean stepping = ctxt.threadState.isStepping();

		// Async calls have the OpRequest made by the caller using the
		// "from" CPU, whereas the OpActivate and OpComplete are made
		// by the called object, using self's CPU (see trace(msg)).

		RTLogger.log(
			"OpRequest -> id: " + Thread.currentThread().getId() +
			" opname: \"" + name + "\"" +
			" objref: " + self.objectReference +
			" clnm: \"" + self.type.name.getName() + "\"" +
			" cpunm: " + from.getNumber() +
			" async: " + isAsync
			);

		if (from != to)		// Remote CPU call
		{
    		BUSValue bus = BUSValue.lookupBUS(from, to);

    		if (bus == null)
    		{
    			abort(4140,
    				"No BUS between CPUs " + from.getName() + " and " + to.getName(), ctxt);
    		}

    		if (isAsync)	// Don't wait
    		{
        		MessageRequest request = new MessageRequest(
        			bus, from, to, self, this, argValues, null, stepping);

        		bus.transmit(request);
        		return new VoidValue();
    		}
    		else
    		{
        		Holder<MessageResponse> result = new Holder<MessageResponse>();
        		MessageRequest request = new MessageRequest(
        			bus, from, to, self, this, argValues, result, stepping);

        		bus.transmit(request);
        		MessageResponse reply = result.get(ctxt, name.getLocation());
        		return reply.getValue();	// Can throw a returned exception
    		}
		}
		else	// local, must be async so don't wait
		{
    		MessageRequest request = new MessageRequest(
    			null, from, to, self, this, argValues, null, stepping);

    		new AsyncThread(request).start();
    		return new VoidValue();
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

			if (val instanceof OperationValue)
			{
				OperationValue ov = (OperationValue)val;
				return ov.type.equals(type);
			}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return type.hashCode();
	}

	@Override
	public String kind()
	{
		return "operation";
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to.isType(TCOperationType.class, to.location))
		{
			return this;
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public OperationValue operationValue(Context ctxt)
	{
		return this;
	}

	@Override
	public Object clone()
	{
		if (expldef != null)
		{
			return new OperationValue(expldef, precondition, postcondition,
				state);
		}
		else
		{
			return new OperationValue(impldef, precondition, postcondition,
				state);
		}
	}

	private synchronized void req(boolean logreq)
	{
		hashReq++;

		if (logreq)		// Async OpRequests are made in asyncEval
		{
			trace("OpRequest");
		}

		debug("#req = " + hashReq);
	}

	private synchronized void act()
	{
		hashAct++;

		if (!ResourceScheduler.isStopping())
		{
			trace("OpActivate");
			debug("#act = " + hashAct);
		}
	}

	private synchronized void fin()
	{
		hashFin++;

		if (!ResourceScheduler.isStopping())
		{
			trace("OpCompleted");
			debug("#fin = " + hashFin);
		}
	}

	private void trace(String kind)
	{
		if (traceRT)
		{
			Thread ct = Thread.currentThread();

			if (isStatic)
			{
				int cpu = 0;

				if (ct instanceof SchedulableThread)
				{
					SchedulableThread th = (SchedulableThread)ct;
					cpu = th.getCPUResource().getNumber();
				}
				else
				{
					cpu = 0;	// Initialization on vCPU
				}

	    		RTLogger.log(
	    			kind + " -> id: " + ct.getId() +
	    			" opname: \"" + name + "\"" +
	    			" objref: nil" +
	    			" clnm: \"" + classdef.name.getName() + "\"" +
	    			" cpunm: " + cpu +
	    			" async: " + isAsync
	    			);
			}
			else
			{
        		RTLogger.log(
        			kind + " -> id: " + ct.getId() +
        			" opname: \"" + name + "\"" +
        			" objref: " + self.objectReference +
        			" clnm: \"" + self.type.name.getName() + "\"" +
        			" cpunm: " + self.getCPU().getNumber() +
        			" async: " + isAsync
        			);
			}
		}
	}

	/**
	 * @param string
	 */
	private void debug(String string)
	{
		if (Properties.diags_guards)
		{
			if (Settings.dialect == Dialect.VDM_PP)
			{
				System.err.println(String.format("%s %s %s",
					Thread.currentThread(), name, string));
			}
			else
			{
				RTLogger.log(String.format("-- %s %s %s",
					Thread.currentThread(), name, string));
			}
		}
	}

	public synchronized void setPriority(long priority)
	{
		this.priority = priority;
	}

	public synchronized long getPriority()
	{
		return priority;
	}

	public synchronized CPUValue getCPU()
	{
		return self == null ? CPUValue.vCPU : self.getCPU();
	}

	public String toTitle()
	{
		return name.getName() + Utils.listToString("(", paramPatterns, ", ", ")");
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseOperationValue(this, arg);
	}
}
