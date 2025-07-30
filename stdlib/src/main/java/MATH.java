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

// This must be in the default package to work with VDMJ's native delegation.

import java.util.Random;

import com.fujitsu.vdmj.runtime.VDMFunction;
import com.fujitsu.vdmj.runtime.VDMOperation;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.NumericValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.Value;

public class MATH
{
	private static Random random = new Random();
	private static long seed = 0;

	@VDMFunction(params = {NumericValue.class})
	public static Value sin(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.sin(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value cos(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.cos(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value tan(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.tan(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value cot(Value arg) throws ValueException, Exception
	{
		return new RealValue(1/Math.tan(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value asin(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.asin(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value acos(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.acos(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value atan(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.atan(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value sqrt(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.sqrt(arg.realValue(null)));
	}

	@VDMFunction
	public static Value pi_f() throws Exception
	{
		return new RealValue(Math.PI);
	}

	@VDMOperation(params = {IntegerValue.class})
	public static Value rand(Value arg) throws ValueException
	{
		long lv = arg.intValue(null);

		if (seed == -1)
		{
			return new IntegerValue(lv);
		}
		else if (lv == 0)
		{
			return new IntegerValue(0);
		}
		else
		{
			return new IntegerValue(Math.abs(random.nextLong() % lv));
		}
	}

	@VDMOperation(params = {IntegerValue.class})
	public static Value srand2(Value arg) throws ValueException
	{
		seed = arg.intValue(null);
		random.setSeed(seed);
		return new IntegerValue(seed);
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value exp(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.exp(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value ln(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.log(arg.realValue(null)));
	}

	@VDMFunction(params = {NumericValue.class})
	public static Value log(Value arg) throws ValueException, Exception
	{
		return new RealValue(Math.log10(arg.realValue(null)));
	}

	@VDMFunction(params = {NaturalValue.class})
	public static Value fac(Value arg) throws ValueException, Exception
	{
		return new NaturalOneValue(factorial(arg.natValue(null)));
	}

	@VDMFunction
	public static Value defined(Value arg) throws ValueException, Exception
	{
		return new BooleanValue(arg.isDefined());
	}

	private static long factorial(long n)
	{
		return (n < 1) ? 1 : n * factorial(n-1);
	}
}
