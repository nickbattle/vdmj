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

import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class IntegerValue extends RationalValue
{
	private static final long serialVersionUID = 1L;
	protected final long longVal;

	public IntegerValue(long value)
	{
		super(value);
		longVal = value;
	}

	@Override
	public int compareTo(Value other)
	{
		other = other.deref();
		
		if (other instanceof IntegerValue)
		{
			IntegerValue io = (IntegerValue)other;
			return (longVal < io.longVal ? -1 : (longVal == io.longVal ? 0 : 1));
		}

		return super.compareTo(other);
	}

	@Override
	public String toString()
	{
		return Long.toString(longVal);
	}

	@Override
	public long intValue(Context ctxt)
	{
		return longVal;
	}

	@Override
	public long nat1Value(Context ctxt) throws ValueException
	{
		if (longVal < 1)
		{
			abort(4058, "Value " + longVal + " is not a nat1", ctxt);
		}

		return longVal;
	}

	@Override
	public long natValue(Context ctxt) throws ValueException
	{
		if (longVal < 0)
		{
			abort(4059, "Value " + longVal + " is not a nat", ctxt);
		}

		return longVal;
	}

	@Override
	public double realValue(Context ctxt)
	{
		return longVal;
	}

	@Override
	public int hashCode()
	{
		return (int)longVal;
	}

	@Override
	public String kind()
	{
		return "int";
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to instanceof TCIntegerType)
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
		try
		{
			return new IntegerValue(longVal);
		}
		catch (Exception e)
		{
			throw new InternalException(5, "Illegal clone");
		}
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIntegerValue(this, arg);
	}
}
