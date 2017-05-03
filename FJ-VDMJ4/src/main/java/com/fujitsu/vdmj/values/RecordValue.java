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

import java.util.Iterator;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;

public class RecordValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final TCRecordType type;
	public final FieldMap fieldmap;
	public final FunctionValue invariant;
	public final FunctionValue equality;
	public final FunctionValue ordering;
	
	private Context compareCtxt = null;

	// mk_ expressions
	public RecordValue(TCRecordType type, ValueList values, Context ctxt) throws ValueException
	{
		this.type = type;
		this.fieldmap = new FieldMap();
		this.invariant = type.getInvariant(ctxt);
		this.equality = type.getEquality(ctxt);
		this.ordering = type.getOrder(ctxt);

		if (values.size() != type.fields.size())
		{
			abort(4078, "Wrong number of fields for " + type.name, ctxt);
		}

		Iterator<TCField> fi = type.fields.iterator();

		for (Value v: values)
		{
			TCField f = fi.next();
			fieldmap.add(f.tag, v.convertTo(f.type, ctxt), !f.equalityAbstration);
		}
		
		checkInvariant(ctxt);
	}

	// mu_ expressions
	public RecordValue(TCRecordType type, FieldMap mapvalues, Context ctxt)
		throws ValueException
	{
		this.type = type;
		this.fieldmap = new FieldMap();
		this.invariant = type.getInvariant(ctxt);
		this.equality = type.getEquality(ctxt);
		this.ordering = type.getOrder(ctxt);

		if (mapvalues.size() != type.fields.size())
		{
			abort(4080, "Wrong number of fields for " + type.name, ctxt);
		}

		Iterator<TCField> fi = type.fields.iterator();

		while (fi.hasNext())
		{
			TCField f = fi.next();
			Value v = mapvalues.get(f.tag);

			if (v == null)
			{
				abort(4081, "ASTField not defined: " + f.tag, ctxt);
			}

			fieldmap.add(f.tag, v.convertTo(f.type, ctxt), !f.equalityAbstration);
		}

		checkInvariant(ctxt);
	}

	// Only called by clone()
	private RecordValue(TCRecordType type, FieldMap mapvalues,
			FunctionValue invariant, FunctionValue equality, FunctionValue ordering)
	{
		this.type = type;
		this.invariant = invariant;
		this.equality = equality;
		this.ordering = ordering;
		this.fieldmap = mapvalues;
	}

	// State records - invariant handled separately and no equality
	public RecordValue(TCRecordType type, NameValuePairList mapvalues)
	{
		this.type = type;
		this.invariant = null;
		this.equality = null;
		this.ordering = null;
		this.fieldmap = new FieldMap();

		for (NameValuePair nvp: mapvalues)
		{
			TCField f = type.findField(nvp.name.getName());
			this.fieldmap.add(nvp.name.getName(), nvp.value, !f.equalityAbstration);
		}
	}
	
	@Override
	public boolean isOrdered()
	{
		return ordering != null;
	}

	public void checkInvariant(Context ctxt) throws ValueException
	{
		if (invariant != null && Settings.invchecks)
		{
			// In VDM++ and VDM-RT, we do not want to do thread swaps half way
			// through an invariant check, so we set the atomic flag around the
			// conversion. This also stops VDM-RT from performing "time step"
			// calculations.
	
			try
			{
				ctxt.threadState.setAtomic(true);
				boolean inv = invariant.eval(invariant.location, this, ctxt).boolValue(ctxt);
		
				if (!inv)
				{
					abort(4079, "Type invariant violated by mk_" + type.name.getName() + " arguments", ctxt);
				}
			}
			finally
			{
				ctxt.threadState.setAtomic(false);
			}
		}
	}

	@Override
	public RecordValue recordValue(Context ctxt)
	{
		return this;
	}

	@Override
	public Value getUpdatable(ValueListenerList listeners)
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

		FieldMap nm = new FieldMap();

		for (FieldValue fv: fieldmap)
		{
			Value uv = fv.value.getUpdatable(listeners);
			nm.add(fv.name, uv, fv.comparable);
		}

		UpdatableValue uval = UpdatableValue.factory(new RecordValue(type, nm, invariant, equality, ordering), listeners);
		
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
		FieldMap nm = new FieldMap();

		for (FieldValue fv: fieldmap)
		{
			Value uv = fv.value.getConstant();
			nm.add(fv.name, uv, fv.comparable);
		}

		return new RecordValue(type, nm, invariant, equality, ordering);
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();
			
    		if (val instanceof RecordValue)
    		{
    			RecordValue ot = (RecordValue)val;
    
    			if (ot.type.equals(type))
    			{
    				if (equality != null)
    				{
    					Context ctxt = compareCtxt != null ?
    						compareCtxt : new Context(ordering.location, "eq", null);
    					ctxt.setThreadState(null);
    					ctxt.threadState.setAtomic(true);

    					try
						{
   	    					ValueList args = new ValueList();
   	    					args.add(this);
   	    					args.add(ot);
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
	    				for (TCField f: type.fields)
	    				{
	    					if (!f.equalityAbstration)
	    					{
	    						Value fv = fieldmap.get(f.tag);
	    						Value ofv = ot.fieldmap.get(f.tag);
	    
	    						if (fv == null || ofv == null)
	    						{
	    							return false;
	    						}
	    
	    						if (!fv.equals(ofv))
	    						{
	    							return false;
	    						}
	    					}
	    				}
    				}
    
    				return true;
    			}
    		}
		}

		return false;
	}

	@Override
	public boolean equals(Object other, Context ctxt)
	{
		compareCtxt = ctxt;
		boolean rv = equals(other);
		compareCtxt = null;
		return rv;
	}

	@Override
	public int compareTo(Value other, Context ctxt)
	{
		compareCtxt = ctxt;
		int rv = compareTo(other);
		compareCtxt = null;
		return rv;
	}

	@Override
	public int compareTo(Value other)
	{
		Value val = other.deref();

		if (val instanceof RecordValue)
		{
			RecordValue ot = (RecordValue)val;

			if (ot.type.equals(type))
			{
				if (ordering != null)
				{
					Context ctxt = compareCtxt != null ?
						compareCtxt : new Context(ordering.location, "ord", null);
					ctxt.setThreadState(null);
					ctxt.threadState.setAtomic(true);

					try
					{
						ValueList args = new ValueList();
						args.add(this);
						args.add(ot);
						
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
				else
				{
					for (TCField f: type.fields)
					{
						if (!f.equalityAbstration)
						{
							Value fv = fieldmap.get(f.tag);
							Value ofv = ot.fieldmap.get(f.tag);
	
							if (fv == null || ofv == null)
							{
								return -1;
							}
	
							int comp = fv.compareTo(ofv);
	
							if (comp != 0)
							{
								return comp;
							}
						}
					}

					return 0;
				}
			}
		}

		return Integer.MIN_VALUE;	// Indicates incomparable values, but allows "sorting"
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("mk_" + type.name + "(");

		Iterator<TCField> fi = type.fields.iterator();

		if (fi.hasNext())
		{
    		String ftag = fi.next().tag;
    		sb.append(fieldmap.get(ftag));

    		while (fi.hasNext())
    		{
    			ftag = fi.next().tag;
    			sb.append(", " + fieldmap.get(ftag));
    		}
		}

		sb.append(")");
		return sb.toString();
	}

	@Override
	public int hashCode()
	{
		return type.name.hashCode() + fieldmap.hashCode();
	}

	@Override
	public String kind()
	{
		return type.toString();
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to.equals(type))
		{
			return this;
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public Object clone()
	{
		return new RecordValue(type, (FieldMap)fieldmap.clone(), invariant, equality, ordering);
	}
}
