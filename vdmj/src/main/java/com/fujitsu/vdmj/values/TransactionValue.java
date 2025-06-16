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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.InvariantListenerEditor;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

/**
 * A class to hold an updatable value that can be modified by VDM-RT
 * threads in transactions, committed at a duration point.
 */
public class TransactionValue extends UpdatableValue
{
	private static final long serialVersionUID = 1L;

	private static List<TransactionValue> commitList = new Vector<TransactionValue>();

	private Value newvalue = null;		// The pending value before a commit
	private long newthreadid = -1;		// The thread that made the change

	protected TransactionValue(Value value, ValueListenerList listeners, TCType type)
	{
		super(value, listeners, type);
		newvalue = value;
	}

	protected TransactionValue(ValueListenerList listeners, TCType type)
	{
		super(listeners, type);
		newvalue = value;
	}

	private Value select()
	{
		if (newthreadid > 0 &&
			Thread.currentThread().getId() == newthreadid)
		{
			return newvalue;
		}
		else
		{
			return value;
		}
	}

	@Override
	public synchronized UpdatableValue getUpdatable(ValueListenerList watch)
	{
		return new TransactionValue(select(), watch, restrictedTo);
	}

	@Override
	protected
	synchronized Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		return select().convertValueTo(to, ctxt, done).getUpdatable(listeners);
	}

	@Override
	public void set(LexLocation location, Value newval, Context ctxt) throws ValueException
	{
		long current = Thread.currentThread().getId();

		if (newthreadid > 0 && current != newthreadid)
		{
			ExceptionHandler.handle(new ContextException(
				4142, "Value already updated by thread " + newthreadid, location, ctxt));
		}

		synchronized (this)
		{
			if (newvalue instanceof InvariantValue && Settings.dialect != Dialect.VDM_SL)
			{
				// Before overwriting this invariant value, we check whether any listeners
				// refer to it, and remove them - these would otherwise become dangling links.
				// See VSCode bug #197. Only affects VDM++/RT because of objrefs.
				
				newvalue.apply(new InvariantListenerEditor(), newvalue);
			}

			UpdatableValue updated = newval.getConstant().getUpdatable(listeners);

    		if (restrictedTo != null)
    		{
				updated = (UpdatableValue) updated.convertTo(restrictedTo, ctxt);
    		}

    		newvalue = updated.value;	// To avoid nested updatables
		}

		if (newthreadid < 0)
		{
			synchronized (commitList)
			{
				newthreadid = Thread.currentThread().getId();
				commitList.add(this);
			}
		}

		if (listeners != null)
		{
			listeners.changedValue(location, newvalue, ctxt);
		}
	}

	public static void commitAll()
	{
		synchronized (commitList)
		{
    		for (TransactionValue v: commitList)
    		{
    			v.commit();
    		}

    		commitList.clear();
		}
	}

	public static void commitOne(long tid)
	{
		synchronized (commitList)
		{
    		ListIterator<TransactionValue> it = commitList.listIterator();

    		while (it.hasNext())
    		{
    			TransactionValue v = it.next();

    			if (v.newthreadid == tid)
    			{
    				v.commit();
    				it.remove();
    			}
    		}
		}
	}

	private void commit()
	{
		if (newthreadid > 0)
		{
			value = newvalue;		// Listener called for original "set"
			newthreadid = -1;
		}
	}

	@Override
	public synchronized Object clone()
	{
		return new TransactionValue((Value)select().clone(), listeners, restrictedTo);
	}

	@Override
	public synchronized boolean isType(Class<? extends Value> valueclass)
	{
		return valueclass.isInstance(select());
	}

	@Override
	public synchronized Value deref()
	{
		return select().deref();
	}

	@Override
	public synchronized Value getConstant()
	{
		return select().getConstant();
	}

	@Override
	public synchronized boolean isUndefined()
	{
		return select().isUndefined();
	}

	@Override
	public synchronized boolean isVoid()
	{
		return select().isVoid();
	}

	@Override
	public synchronized BigDecimal realValue(Context ctxt) throws ValueException
	{
		return select().realValue(ctxt);
	}

	@Override
	public synchronized BigInteger intValue(Context ctxt) throws ValueException
	{
		return select().intValue(ctxt);
	}

	@Override
	public synchronized BigInteger natValue(Context ctxt) throws ValueException
	{
		return select().nat1Value(ctxt);
	}

	@Override
	public synchronized BigInteger nat1Value(Context ctxt) throws ValueException
	{
		return select().nat1Value(ctxt);
	}

	@Override
	public synchronized boolean boolValue(Context ctxt) throws ValueException
	{
		return select().boolValue(ctxt);
	}

	@Override
	public synchronized char charValue(Context ctxt) throws ValueException
	{
		return select().charValue(ctxt);
	}

	@Override
	public synchronized ValueList tupleValue(Context ctxt) throws ValueException
	{
		return select().tupleValue(ctxt);
	}

	@Override
	public synchronized RecordValue recordValue(Context ctxt) throws ValueException
	{
		return select().recordValue(ctxt);
	}

	@Override
	public synchronized ObjectValue objectValue(Context ctxt) throws ValueException
	{
		return select().objectValue(ctxt);
	}

	@Override
	public synchronized String quoteValue(Context ctxt) throws ValueException
	{
		return select().quoteValue(ctxt);
	}

	@Override
	public synchronized ValueList seqValue(Context ctxt) throws ValueException
	{
		return select().seqValue(ctxt);
	}

	@Override
	public synchronized ValueSet setValue(Context ctxt) throws ValueException
	{
		return select().setValue(ctxt);
	}

	@Override
	public synchronized String stringValue(Context ctxt) throws ValueException
	{
		return select().stringValue(ctxt);
	}

	@Override
	public synchronized ValueMap mapValue(Context ctxt) throws ValueException
	{
		return select().mapValue(ctxt);
	}

	@Override
	public synchronized FunctionValue functionValue(Context ctxt) throws ValueException
	{
		return select().functionValue(ctxt);
	}

	@Override
	public synchronized OperationValue operationValue(Context ctxt) throws ValueException
	{
		return select().operationValue(ctxt);
	}

	@Override
	public synchronized boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof TransactionValue)
    		{
    			TransactionValue tvo = (TransactionValue)val;
    			return select().equals(tvo.select());
    		}
    		else if (val instanceof ReferenceValue)
    		{
    			ReferenceValue rvo = (ReferenceValue)val;
    			return select().equals(rvo.value);
    		}
    		else
    		{
    			return select().equals(other);
    		}
		}

		return false;
	}

	@Override
	public synchronized String kind()
	{
		return select().kind();
	}

	@Override
	public synchronized int hashCode()
	{
		return select().hashCode();
	}

	@Override
	public synchronized String toString()
	{
		return select().toString();
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTransactionValue(this, arg);
	}
}
