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

import java.util.FormattableFlags;
import java.util.Formatter;
import java.util.Iterator;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;


public class TupleValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final ValueList values;

	public TupleValue(ValueList argvals)
	{
		this.values = argvals;
	}

	@Override
	public ValueList tupleValue(Context ctxt)
	{
		return values;
	}

	@Override
	public UpdatableValue getUpdatable(ValueListenerList listeners)
	{
		ValueList ntup = new ValueList();

		for (Value k: values)
		{
			Value v = k.getUpdatable(listeners);
			ntup.add(v);
		}

		return UpdatableValue.factory(new TupleValue(ntup), listeners);
	}

	@Override
	public Value getConstant()
	{
		ValueList ntup = new ValueList();

		for (Value k: values)
		{
			Value v = k.getConstant();
			ntup.add(v);
		}

		return new TupleValue(ntup);
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof TupleValue)
    		{
    			TupleValue ot = (TupleValue)val;
    			return values.equals(ot.values);
    		}
		}

		return false;
	}

	@Override
	public int compareTo(Value other)
	{
		if (other instanceof TupleValue)
		{
			TupleValue ot = (TupleValue)other;
			int diff = values.size() - ot.values.size();

			if (diff != 0)
			{
				return diff;
			}
			else
			{
				for (int i=0; i<values.size();i++)
				{
					int c = values.get(i).compareTo(ot.values.get(i));

					if (c != 0)
					{
						return c;
					}
				}

				return 0;
			}
		}

		return super.compareTo(other);
	}

	@Override
	public String toString()
	{
		return "mk_(" + Utils.listToString(values) + ")";
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision)
	{
		String s = toString();		// With 'mk_(...)', by default

		if ((flags & FormattableFlags.ALTERNATE) > 0)
		{
			s = Utils.listToString(values);
			flags = flags & ~FormattableFlags.ALTERNATE;
		}

		formatTo(s, formatter, flags, width, precision);
	}

	@Override
	public int hashCode()
	{
		return values.hashCode();
	}

	@Override
	public String kind()
	{
		return "tuple";
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to instanceof TCProductType)
		{
			TCProductType pto = (TCProductType)to;

			if (pto.types.size() != values.size())
			{
				abort(4085, "Cannot convert tuple to " + to, ctxt);
			}

			ValueList nl = new ValueList();
			Iterator<Value> vi = values.iterator();

			for (TCType pt: pto.types)
			{
				nl.add(vi.next().convertValueTo(pt, ctxt));
			}

			return new TupleValue(nl);
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public Object clone()
	{
		return new TupleValue((ValueList)values.clone());
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTupleValue(this, arg);
	}
}
