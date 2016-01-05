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
import java.math.RoundingMode;

import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.types.IntegerType;
import org.overturetool.vdmj.types.NaturalOneType;
import org.overturetool.vdmj.types.NaturalType;
import org.overturetool.vdmj.types.RationalType;
import org.overturetool.vdmj.types.RealType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeSet;

public abstract class NumericValue extends Value
{
	private static final long serialVersionUID = 1L;
	public final BigDecimal value;

	public NumericValue(BigDecimal value)
	{
		super();
		this.value = value;
	}

	public static NumericValue valueOf(BigDecimal d, Context ctxt) throws ValueException
	{
//		if (Double.isInfinite(d) || Double.isNaN(d))
//		{
//			throw new ValueException(4134, "Infinite or NaN trouble", ctxt);
//		}

		BigInteger rounded = d.setScale(0, RoundingMode.HALF_UP).toBigInteger();

		if (new BigDecimal(rounded).compareTo(d) != 0)
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

	public static NumericValue valueOf(BigInteger iv, Context ctxt) throws ValueException
	{
		if (iv.compareTo(BigInteger.ZERO) > 0)
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

		if (iv.compareTo(BigInteger.ZERO) >= 0)
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
	
	public static boolean isInteger(Value value)
	{
		return value instanceof IntegerValue;
	}

	public static boolean areIntegers(Value left, Value right)
	{
		return isInteger(left) && isInteger(right);
	}

	@Override
	protected Value convertValueTo(Type to, Context ctxt, TypeSet done) throws ValueException
	{
		if (to instanceof RealType)
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
		else if (to instanceof RationalType)
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
		else if (to instanceof IntegerType)
		{
			return new IntegerValue(intValue(ctxt));
		}
		else if (to instanceof NaturalType)
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
		else if (to instanceof NaturalOneType)
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
				return nov.value.compareTo(value) == 0;		// NB. NOT equals()
			}
		}

		return false;
	}

	@Override
	abstract public BigDecimal realValue(Context ctxt) throws ValueException;
	@Override
	abstract public BigDecimal ratValue(Context ctxt) throws ValueException;
	@Override
	abstract public BigInteger intValue(Context ctxt) throws ValueException;
	@Override
	abstract public BigInteger natValue(Context ctxt) throws ValueException;
	@Override
	abstract public BigInteger nat1Value(Context ctxt) throws ValueException;
	@Override
	abstract public int hashCode();
	@Override
	abstract public String toString();
}
