/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.values;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.overturetool.vdmj.Settings;
import org.overturetool.vdmj.messages.InternalException;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.types.IntegerType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;

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
		if (other instanceof IntegerValue)
		{
			IntegerValue io = (IntegerValue)other;
			return longVal.compareTo(io.longVal);
		}

		return super.compareTo(other);
	}

	@Override
	public String toString()
	{
		return longVal.toString();
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
		return new BigDecimal(longVal).setScale(
			Settings.precision.getPrecision(), Settings.precision.getRoundingMode());
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
	protected Value convertValueTo(Type to, Context ctxt, TypeSet done) throws ValueException
	{
		if (to instanceof IntegerType)
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
}
