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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.RationalValue;

import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class IntegerValue extends RationalValue
{
	private static final long serialVersionUID = 1L;
	protected final BigInteger longVal;

	public IntegerValue(BigInteger value)
	{
		super(value);
		longVal = value;
	}

	public IntegerValue(long value)
	{
		this(new BigInteger(Long.toString(value)));
	}

	@Override
	public int compareTo(Value other)
	{
		other = other.deref();
		
		if (other instanceof IntegerValue)
		{
			IntegerValue io = (IntegerValue)other;
			return longVal.compareTo(io.longVal);
		}

		return super.compareTo(other);
	}

	private static final int TRAILING_ZERO_LIMIT = 10;
	
	@Override
	public String toString()
	{
		String s = longVal.toString();
		
		// Some large integer values are of the form 1234500000...0000, which
		// is hard to read. So if the string value ends in enough zeros, we
		// print the value via a BigDecimal (in exponential form) instead.
		
		int zeros = 0;
		
		for (int i = s.length()-1; i >= 0; i--)
		{
			if (s.charAt(i) == '0')
			{
				zeros++;
			}
			else
			{
				if (zeros > TRAILING_ZERO_LIMIT)
				{
					return new BigDecimal(longVal).stripTrailingZeros().toString();
				}
				else
				{
					break;
				}
			}
		}
		
		return s;
	}

	@Override
	public BigInteger intValue(Context ctxt)
	{
		return longVal;
	}

	@Override
	public BigInteger nat1Value(Context ctxt) throws ValueException
	{
		if (longVal.compareTo(BigInteger.ONE) < 0)
		{
			abort(4058, "Value " + longVal + " is not a nat1", ctxt);
		}

		return longVal;
	}

	@Override
	public BigInteger natValue(Context ctxt) throws ValueException
	{
		if (longVal.compareTo(BigInteger.ZERO) < 0)
		{
			abort(4059, "Value " + longVal + " is not a nat", ctxt);
		}

		return longVal;
	}

	@Override
	public BigDecimal realValue(Context ctxt)
	{
		return new BigDecimal(longVal, Settings.precision);
	}

	@Override
	public int hashCode()
	{
		return longVal.hashCode();
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
