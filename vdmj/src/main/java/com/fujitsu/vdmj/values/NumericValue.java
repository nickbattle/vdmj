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

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public abstract class NumericValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final double value;

	public NumericValue(double value)
	{
		super();
		this.value = value;
	}

	public static NumericValue valueOf(double d, Context ctxt) throws ValueException
	{
		if (Double.isInfinite(d) || Double.isNaN(d))
		{
			throw new ValueException(4134, "Infinite or NaN trouble", ctxt);
		}

		long rounded = Math.round(d);

		if (rounded != d)
		{
			try
			{
				return new RealValue(d);
			}
			catch (Exception e)
			{
				throw new ValueException(4134, e.getMessage(), ctxt);
			}
		}

		return valueOf(rounded, ctxt);
	}

	public static NumericValue valueOf(long iv, Context ctxt) throws ValueException
	{
		if (iv > 0)
		{
			try
			{
				return new NaturalOneValue(iv);
			}
			catch (Exception e)
			{
				throw new ValueException(4064, e.getMessage(), ctxt);
			}
		}

		if (iv >= 0)
		{
			try
			{
				return new NaturalValue(iv);
			}
			catch (Exception e)
			{
				throw new ValueException(4065, e.getMessage(), ctxt);
			}
		}

		return new IntegerValue(iv);
	}

	public static boolean areIntegers(Value l, Value r)
	{
		return (l instanceof IntegerValue && r instanceof IntegerValue);
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to instanceof TCRealType)
		{
			try
			{
				return new RealValue(realValue(ctxt));
			}
			catch (Exception e)
			{
				throw new ValueException(4134, e.getMessage(), ctxt);
			}
		}
		else if (to instanceof TCRationalType)
		{
			try
			{
				return new RationalValue(ratValue(ctxt));
			}
			catch (Exception e)
			{
				throw new ValueException(4134, e.getMessage(), ctxt);
			}
		}
		else if (to instanceof TCIntegerType)
		{
			return new IntegerValue(intValue(ctxt));
		}
		else if (to instanceof TCNaturalType)
		{
			try
			{
				return new NaturalValue(natValue(ctxt));
			}
			catch (Exception e)
			{
				return abort(4065, e.getMessage(), ctxt);
			}
		}
		else if (to instanceof TCNaturalOneType)
		{
			try
			{
				return new NaturalOneValue(nat1Value(ctxt));
			}
			catch (Exception e)
			{
				return abort(4064, e.getMessage(), ctxt);
			}
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

			if (val instanceof NumericValue)
			{
				NumericValue nov = (NumericValue)val;
				return nov.value == value;
			}
		}

		return false;
	}

	@Override
	abstract public double realValue(Context ctxt) throws ValueException;
	@Override
	abstract public double ratValue(Context ctxt) throws ValueException;
	@Override
	abstract public long intValue(Context ctxt) throws ValueException;
	@Override
	abstract public long natValue(Context ctxt) throws ValueException;
	@Override
	abstract public long nat1Value(Context ctxt) throws ValueException;
	@Override
	abstract public int hashCode();
	@Override
	abstract public String toString();

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNumericValue(this, arg);
	}
}
