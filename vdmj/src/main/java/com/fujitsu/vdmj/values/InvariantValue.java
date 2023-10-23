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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class InvariantValue extends ReferenceValue
{
	private static final long serialVersionUID = 1L;
	public final TCNamedType type;
	public final FunctionValue invariant;
	public final FunctionValue equality;
	public final FunctionValue ordering;
	
	public InvariantValue(TCNamedType type, Value value, Context ctxt)
		throws ValueException
	{
		super(value);
		this.type = type;

		this.invariant = type.getInvariant(ctxt);
		this.equality = type.getEquality(ctxt);
		this.ordering = type.getOrder(ctxt);
		checkInvariant(ctxt);
	}

	public void checkInvariant(Context ctxt) throws ValueException
	{
		if (invariant != null && Settings.invchecks && !type.isMaximal())
		{
			// In VDM++ and VDM-RT, we do not want to do thread swaps half way
			// through a DTC check (which can include calculating an invariant),
			// so we set the atomic flag around the conversion. This also stops
			// VDM-RT from performing "time step" calculations.

			boolean inv = false;
			
			try
			{
				ctxt.threadState.setAtomic(true);
				inv = invariant.eval(invariant.location, value, ctxt).boolValue(ctxt);
			}
			catch (ValueException e)
			{
				ExceptionHandler.handle(new ContextException(4060, e.getMessage(), invariant.location, ctxt));
			}
			finally
			{
				ctxt.threadState.setAtomic(false);
			}

			if (!inv)
			{
				abort(4060, "Type invariant violated for " + type.typename, ctxt);
			}
		}
	}

	// For clone only
	private InvariantValue(TCNamedType type, Value value, FunctionValue invariant,
		FunctionValue equality, FunctionValue ordering)
	{
		super(value);
		this.type = type;
		this.invariant = invariant;
		this.equality = equality;
		this.ordering = ordering;
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to.equals(type))
		{
			if (type.isMaximal() != to.isMaximal())
			{
				return value.convertValueTo(to, ctxt, done);
			}
			else
			{
				return this;
			}
		}
		else
		{
			return value.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public UpdatableValue getUpdatable(ValueListenerList listeners)
	{
		InvariantValueListener invl = null;

		if (invariant != null)
		{
			// Add an invariant listener to a new list for children of this value
			// We update the object in the listener once we've created it (below)

			invl = new InvariantValueListener();
			ValueListenerList list = new ValueListenerList(invl);

			if (listeners != null)
			{
				list.addAll(listeners);
			}

			listeners = list;
		}

		InvariantValue ival = new InvariantValue(type, value.getUpdatable(listeners), invariant, equality, ordering);
		UpdatableValue uval = UpdatableValue.factory(ival, listeners);

		if (invl != null)
		{
			// Update the listener with the address of the updatable copy
			invl.setValue(uval);
		}

		return uval;
	}

	@Override
	public Value getConstant()
	{
		return new InvariantValue(type, value.getConstant(), invariant, equality, ordering);
	}

	@Override
	public Object clone()
	{
		return new InvariantValue(type, (Value)value.clone(), invariant, equality, ordering);
	}

	@Override
	public int compareTo(Value other)
	{
		if (ordering != null && !type.isMaximal() &&
			other instanceof InvariantValue &&
			((InvariantValue)other).type.equals(type))
		{
			// To avoid inheriting prepost settings from the global context, we copy it
			Context ctxt = new Context(ordering.location, "ordering evaluation", null);
			ctxt.putAll(Interpreter.getInstance().getInitialContext());
			ctxt.setThreadState(null);
			ctxt.threadState.setAtomic(true);

			try
			{
				ValueList args = new ValueList();
				args.add(this);
				args.add(other);
				
				if (ordering.eval(ordering.location, args, ctxt).boolValue(ctxt))
				{
					return -1;	// Less
				}
				else if (equals(other))
				{
					return 0;	// Equal
				}
				else
				{
					return 1;	// More
				}
			}
			catch (ValueException e)
			{
				throw new RuntimeException(e);
			}
			finally
			{
				ctxt.threadState.setAtomic(false);
			}
		}
		else if (equality != null && equals(other))
		{
			// Works with Maps of invariants that define "eq" (ValueMap is a TreeMap)
			return 0;
		}
		else
		{
			return super.compareTo(other);
		}
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
    		if (equality != null && !type.isMaximal())
    		{
    			if (other instanceof NilValue && !(type.type instanceof TCOptionalType))
    			{
    				return false;
    			}
    			
    			// To avoid inheriting prepost settings from the global context, we copy it
    			Context ctxt = new Context(equality.location, "equals evaluation", null);
    			ctxt.putAll(Interpreter.getInstance().getInitialContext());
    			ctxt.setThreadState(null);
    			ctxt.threadState.setAtomic(true);
    
    			try
    			{
    				ValueList args = new ValueList();
    				args.add(this);
    				args.add((Value)other);
    				return equality.eval(equality.location, args, ctxt).boolValue(ctxt);
    			}
    			catch (ValueException e)
    			{
    				throw new RuntimeException(e);
    			}
    			finally
    			{
    				ctxt.threadState.setAtomic(false);
    			}
    		}
    		else
    		{
    			return super.equals(other);
    		}
		}
		
		return false;
	}
	
	@Override
	public boolean isOrdered()
	{
		return (ordering != null) ? true : value.isOrdered();
	}

	@Override
	public int hashCode()
	{
		if (equality != null)
		{
			// We have to have a hashCode that is consistent with the equality function
			// (ie. the equals method), but since we cannot distinguish unequal values,
			// all we can do to be consistent with the contract is return a fixed value.
			return 0;
		}
		else
		{
			return super.hashCode();
		}
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseInvariantValue(this, arg);
	}
}
