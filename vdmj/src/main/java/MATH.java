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

// This must be in the default package to work with VDMJ's native delegation.

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.Value;

public class MATH
{
	private static Random random = new Random();
	private static long seed = 0;

	public static Value sin(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.sin(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value cos(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.cos(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value tan(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.tan(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value cot(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = Apfloat.ONE.divide(ApfloatMath.tan(ap));
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value asin(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.asin(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value acos(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.acos(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value atan(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.atan(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value sqrt(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.sqrt(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value pi_f() throws Exception
	{
		Apfloat apResult = ApfloatMath.pi(Settings.precision.getPrecision());
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value rand(Value arg) throws Exception
	{
		BigInteger lv = arg.intValue(null).abs();

		if (seed == -1)
		{
			return new IntegerValue(lv);
		}
		else if (lv.equals(BigInteger.ZERO))
		{
			return new IntegerValue(BigInteger.ZERO);
		}
		else
		{
			return new IntegerValue(new BigInteger(lv.bitLength(), random).mod(lv));
		}
	}

	public static Value srand2(Value arg) throws ValueException
	{
		seed = arg.intValue(null).longValue();
		random.setSeed(seed);
		return new IntegerValue(seed);
	}

	public static Value exp(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.exp(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value ln(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.log(ap);
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value log(Value arg) throws ValueException, Exception
	{
		Apfloat ap = new Apfloat(arg.realValue(null), Settings.precision.getPrecision());
		Apfloat apResult = ApfloatMath.log(ap, new Apfloat(10));
		return new RealValue(new BigDecimal(apResult.toString(), Settings.precision));
	}

	public static Value fac(Value arg) throws ValueException, Exception
	{
		return new NaturalOneValue(factorial(arg.natValue(null)));
	}

	private static BigInteger factorial(BigInteger n)
	{
		return (n.compareTo(BigInteger.ONE) < 0) ?
			BigInteger.ONE : n.multiply(factorial(n.subtract(BigInteger.ONE)));
	}
}
