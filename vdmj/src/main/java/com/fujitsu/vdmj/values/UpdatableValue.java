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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.values;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.InvariantListenerEditor;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

/**
 * A class to hold an updatable value. This is almost identical to a
 * ReferenceValue, except that is has a set method which changes the
 * referenced value and calls a listener, and all the remaining methods
 * here are synchronized, to guarantee that the sets and gets see all
 * changes (to the same UpdateableValue) produced by other threads.
 */
public class UpdatableValue extends ReferenceValue
{
	private static final long serialVersionUID = 1L;
	public ValueListenerList listeners;
	protected final TCType restrictedTo;

	public static UpdatableValue factory(Value value, ValueListenerList listeners)
	{
		return factory(value, listeners, null);
	}
	
	public static UpdatableValue factory(Value value, ValueListenerList listeners, TCType type)
	{
		if (Settings.dialect == Dialect.VDM_RT &&
			Properties.rt_duration_transactions)
		{
			return new TransactionValue(value, listeners, type);
		}
		else
		{
			return new UpdatableValue(value, listeners, type);
		}
	}

	public static UpdatableValue factory(ValueListenerList listeners)
	{
		return factory(listeners, null);
	}
	
	public static UpdatableValue factory(ValueListenerList listeners, TCType type)
	{
		if (Settings.dialect == Dialect.VDM_RT &&
			Properties.rt_duration_transactions)
		{
			return new TransactionValue(listeners, type);
		}
		else
		{
			return new UpdatableValue(listeners, type);
		}
	}

	protected UpdatableValue(Value value, ValueListenerList listeners, TCType type)
	{
		super(value);
		this.listeners = listeners;
		this.restrictedTo = type;
	}

	protected UpdatableValue(ValueListenerList listeners, TCType type)
	{
		super();
		this.listeners = listeners;
		this.restrictedTo = type;
	}
	
	@Override
	public synchronized UpdatableValue getUpdatable(ValueListenerList watch)
	{
		// Create new object every time, because we end up with two references
		// to the same value otherwise (Overture bug #544)
		return UpdatableValue.factory(value, watch);
	}

	@Override
	public synchronized Value getConstant()
	{
		return value.getConstant();
	}

	@Override
	protected
	synchronized Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		return value.convertValueTo(to, ctxt, done).getUpdatable(listeners);
	}

	@Override
	public void set(LexLocation location, Value newval, Context ctxt) throws ValueException
	{
		// First make the change without invoking the listeners.
		atomicSet(location, newval, ctxt);
		
		// The listeners are outside the sync in atomicSet because they have to lock
		// the object they notify, which can be holding a lock on this one.

		if (listeners != null)
		{
			listeners.changedValue(location, value, ctxt);
		}
	}
	
	/**
	 * This is used by the atomic statement, and is used to set values
	 * without invoking the invariant checks.
	 */
	public void atomicSet(LexLocation location, Value newval, Context ctxt) throws ValueException
	{
		// Anything with structure added to an UpdateableValue has to be
		// updatable, otherwise you can "freeze" part of the substructure
		// such that it can't be changed. And we have to set the listeners
		// to be "our" listeners, regardless of any it had before.

		synchronized (this)
		{
			if (value instanceof InvariantValue && Settings.dialect != Dialect.VDM_SL)
			{
				// Before overwriting this invariant value, we check whether any listeners
				// refer to it, and remove them - these would otherwise become dangling links.
				// See VSCode bug #197. Only affects VDM++/RT because of objrefs.
				
				value.apply(new InvariantListenerEditor(), value);
			}
			
   			UpdatableValue updated = newval.getConstant().getUpdatable(listeners);
    		
    		if (restrictedTo != null)
    		{
				updated = (UpdatableValue) updated.convertTo(restrictedTo, ctxt);
    		}

    		value = updated.value;	// To avoid nested updatables
		}
		
		// NOTE: we omit the listener check here, called in "set" above.
	}

	public void addListener(ValueListener listener)
	{
		if (listeners != null)
		{
			if (!listeners.contains(listener))
			{
				listeners.add(listener);
			}
		}
		else
		{
			listeners = new ValueListenerList(listener);
		}
	}

	public void addListeners(ValueListenerList list)
	{
		if (listeners == list)	// Same list
		{
			return;
		}
		else if (listeners != null)
		{
			for (ValueListener vl: list)
			{
				if (!listeners.contains(vl))
				{
					listeners.add(vl);
				}
			}
		}
		else
		{
			listeners = new ValueListenerList(list);
		}
	}

	@Override
	public synchronized Object clone()
	{
		return new UpdatableValue((Value)value.clone(), listeners, restrictedTo);
	}

	@Override
	public synchronized boolean isType(Class<? extends Value> valueclass)
	{
		return valueclass.isInstance(value.deref());
	}

	@Override
	public synchronized Value deref()
	{
		return value.deref();
	}

	@Override
	public synchronized boolean isUndefined()
	{
		return value.isUndefined();
	}

	@Override
	public synchronized boolean isVoid()
	{
		return value.isVoid();
	}

	@Override
	public synchronized boolean isNumeric()
	{
		return value.isNumeric();
	}

	@Override
	public synchronized boolean isOrdered()
	{
		return value.isOrdered();
	}

	@Override
	public synchronized double realValue(Context ctxt) throws ValueException
	{
		return value.realValue(ctxt);
	}

	@Override
	public synchronized long intValue(Context ctxt) throws ValueException
	{
		return value.intValue(ctxt);
	}

	@Override
	public synchronized long natValue(Context ctxt) throws ValueException
	{
		return value.nat1Value(ctxt);
	}

	@Override
	public synchronized long nat1Value(Context ctxt) throws ValueException
	{
		return value.nat1Value(ctxt);
	}

	@Override
	public synchronized boolean boolValue(Context ctxt) throws ValueException
	{
		return value.boolValue(ctxt);
	}

	@Override
	public synchronized char charValue(Context ctxt) throws ValueException
	{
		return value.charValue(ctxt);
	}

	@Override
	public synchronized ValueList tupleValue(Context ctxt) throws ValueException
	{
		return value.tupleValue(ctxt);
	}

	@Override
	public synchronized RecordValue recordValue(Context ctxt) throws ValueException
	{
		return value.recordValue(ctxt);
	}

	@Override
	public synchronized ObjectValue objectValue(Context ctxt) throws ValueException
	{
		return value.objectValue(ctxt);
	}

	@Override
	public synchronized String quoteValue(Context ctxt) throws ValueException
	{
		return value.quoteValue(ctxt);
	}

	@Override
	public synchronized ValueList seqValue(Context ctxt) throws ValueException
	{
		return value.seqValue(ctxt);
	}

	@Override
	public synchronized ValueSet setValue(Context ctxt) throws ValueException
	{
		return value.setValue(ctxt);
	}

	@Override
	public synchronized String stringValue(Context ctxt) throws ValueException
	{
		return value.stringValue(ctxt);
	}

	@Override
	public synchronized ValueMap mapValue(Context ctxt) throws ValueException
	{
		return value.mapValue(ctxt);
	}

	@Override
	public synchronized FunctionValue functionValue(Context ctxt) throws ValueException
	{
		return value.functionValue(ctxt);
	}

	@Override
	public synchronized OperationValue operationValue(Context ctxt) throws ValueException
	{
		return value.operationValue(ctxt);
	}

	@Override
	public synchronized boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof ReferenceValue)
    		{
    			ReferenceValue rvo = (ReferenceValue)val;
    			return value.equals(rvo.value);
    		}
    		else
    		{
    			return value.equals(other);
    		}
		}

		return false;
	}

	@Override
	public synchronized String kind()
	{
		return value.kind();
	}

	@Override
	public synchronized int hashCode()
	{
		return value.hashCode();
	}

	@Override
	public synchronized String toString()
	{
		return value.toString();
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseUpdatableValue(this, arg);
	}
}
