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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class RealValue extends NumericValue
{
	private static final long serialVersionUID = 1L;

	public RealValue(BigDecimal value) throws Exception
	{
		super(value);

//		if (Double.isInfinite(value))
//		{
//			throw new Exception("Real is infinite");
//		}
//		else if (Double.isNaN(value))
//		{
//			throw new Exception("Real is NaN");
//		}
	}

	public RealValue(BigInteger value)
	{
		super(new BigDecimal(value, Settings.precision));
	}

	public RealValue(double value)
	{
		super(new BigDecimal(value));
	}

	@Override
	public int compareTo(Value other)
	{
		other = other.deref();
		
		if (other instanceof RealValue)
		{
			RealValue ro = (RealValue)other;
			return value.compareTo(ro.value);
		}

		return super.compareTo(other);
	}

	@Override
	public BigDecimal realValue(Context ctxt)
	{
		return value;
	}

	@Override
	public BigDecimal ratValue(Context ctxt)
	{
		return value;
	}

	@Override
	public BigInteger intValue(Context ctxt) throws ValueException
	{
		BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP);

		if (rounded.compareTo(value) != 0)
		{
			abort(4075, "Value " + value.stripTrailingZeros() + " is not an integer", ctxt);
		}

		return rounded.toBigInteger();
	}

	@Override
	public BigInteger nat1Value(Context ctxt) throws ValueException
	{
		BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP);

		if (rounded.compareTo(value) != 0 || rounded.compareTo(BigDecimal.ONE) < 0)
		{
			abort(4076, "Value " + value.stripTrailingZeros() + " is not a nat1", ctxt);
		}

		return rounded.toBigInteger();
	}

	@Override
	public BigInteger natValue(Context ctxt) throws ValueException
	{
		BigDecimal rounded = value.setScale(0, RoundingMode.HALF_UP);

		if (rounded.compareTo(value) != 0 || rounded.compareTo(BigDecimal.ONE) < 0)
		{
			abort(4077, "Value " + value.stripTrailingZeros() + " is not a nat", ctxt);
		}

		return rounded.toBigInteger();
	}

	@Override
	public String toString()
	{
		return value.stripTrailingZeros().toString();
	}

	@Override
	public int hashCode()
	{
		return value.hashCode();
	}

	@Override
	public String kind()
	{
		return "real";
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to instanceof TCRealType)
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
			return new RealValue(value);
		}
		catch (Exception e)
		{
			// Can't happen?
			return null;
		}
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseRealValue(this, arg);
	}
}
