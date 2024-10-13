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
import java.util.List;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;


public class SetValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final ValueSet values;

	public SetValue()
	{
		this.values = new ValueSet();
	}

	public SetValue(ValueSet values)
	{
		// We arrange that VDMJ set values usually have sorted contents.
		// This guarantees deterministic behaviour in places that would
		// otherwise be variable.

		this(values, true);
	}

	public SetValue(ValueSet values, boolean sort)
	{
		if (sort)
		{
			// The ordering here can throw a ValueException in cases where an
			// order exists over a union of types and some members of the union
			// do not match the type of the ord_T function parameters. Throwing
			// an exception here allows the union convertTo to choose another
			// type from the union, until one succeeds.

			values.sort();
		}

		this.values = values;
	}

	@Override
	public ValueSet setValue(Context ctxt)
	{
		return values;
	}

	@Override
	public UpdatableValue getUpdatable(ValueListenerList listeners)
	{
		ValueSet nset = new ValueSet();

		for (Value k: values)
		{
			Value v = k.getUpdatable(listeners);
			nset.addNoSort(v);
		}

		return UpdatableValue.factory(new SetValue(nset, !values.isSorted()), listeners);
	}

	@Override
	public Value getConstant()
	{
		ValueSet nset = new ValueSet();

		for (Value k: values)
		{
			Value v = k.getConstant();
			nset.addNoSort(v);
		}

		return new SetValue(nset, !values.isSorted());
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof SetValue)
    		{
    			SetValue ot = (SetValue)val;
    			return values.equals(ot.values);
    		}
		}

		return false;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision)
	{
		String s = values.toString();

		if ((flags & FormattableFlags.ALTERNATE) > 0)
		{
			if (values.isEmpty())
			{
				s = "";
			}
			else
			{
				s = s.substring(1, s.length()-1);	// Without "quotes" or "{ ... }"
			}

			flags = flags & ~FormattableFlags.ALTERNATE;
		}

		formatTo(s, formatter, flags, width, precision);
	}

	@Override
	public String toString()
	{
		return values.toString();
	}

	@Override
	public int hashCode()
	{
		return values.hashCode();
	}

	public ValueList permutedSets()
	{
		List<ValueSet> psets = values.permutedSets();
		ValueList rs = new ValueList(psets.size());

		for (ValueSet v: psets)
		{
			rs.add(new SetValue(v, false));		// NB not re-sorted!
		}

		return rs;
	}

	@Override
	public String kind()
	{
		return "set";
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to instanceof TCSetType)
		{
			if (to instanceof TCSet1Type && values.isEmpty())
			{
				abort(4170, "Cannot convert empty set to set1", ctxt);
			}

			TCSetType setto = (TCSetType)to;
			ValueSet ns = new ValueSet();

			for (Value v: values)
			{
				ns.addNoSort(v.convertValueTo(setto.setof, ctxt));
			}

			return new SetValue(ns, true);	// Re-sort, as new type's ord_T may be different
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public Object clone()
	{
		return new SetValue((ValueSet)values.clone(), false);
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetValue(this, arg);
	}
}
