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

import java.io.Serializable;

import com.fujitsu.vdmj.runtime.VDMFunction;
import com.fujitsu.vdmj.runtime.VDMOperation;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;

/**
 * This class delegates native operations to the IO/Math/VDMUtil classes for
 * flat specifications.
 */
public class DEFAULT implements Serializable
{
    private static final long serialVersionUID = 1L;

    //
    // IO...
    //

    @VDMFunction
	public static Value writeval(Value tval)
	{
		return IO.writeval(tval);
	}

    @VDMFunction
	public static Value fwriteval(Value fval, Value tval, Value dval)
	{
		return IO.fwriteval(fval, tval, dval);
	}

//  This function isn't called natively. See INNotYetSpecifiedExpression.
//
//	public static Value freadval(Value fval)
//	{
//		return IO.freadval(fval);
//	}

    @VDMOperation
	public static Value fecho(Value fval, Value tval, Value dval)
	{
		return IO.fecho(fval, tval, dval);
	}

    @VDMOperation
	public static Value ferror()
	{
		return IO.ferror();
	}

    @VDMOperation
	public static Value print(Value v)
	{
		return IO.print(v);
	}

    @VDMOperation
	public static Value println(Value v)
	{
		return IO.println(v);
	}

    @VDMOperation
	public static Value printf(Value fv, Value vs)
		throws ValueException
	{
		return IO.printf(fv, vs);
	}

	//
	// MATH...
	//

    @VDMFunction
	public static Value sin(Value arg) throws ValueException, Exception
	{
		return MATH.sin(arg);
	}

    @VDMFunction
	public static Value cos(Value arg) throws ValueException, Exception
	{
		return MATH.cos(arg);
	}

    @VDMFunction
	public static Value tan(Value arg) throws ValueException, Exception
	{
		return MATH.tan(arg);
	}

    @VDMFunction
	public static Value cot(Value arg) throws ValueException, Exception
	{
		return MATH.cot(arg);
	}

    @VDMFunction
	public static Value asin(Value arg) throws ValueException, Exception
	{
		return MATH.asin(arg);
	}

    @VDMFunction
	public static Value acos(Value arg) throws ValueException, Exception
	{
		return MATH.acos(arg);
	}

    @VDMFunction
	public static Value atan(Value arg) throws ValueException, Exception
	{
		return MATH.atan(arg);
	}

    @VDMFunction
	public static Value sqrt(Value arg) throws ValueException, Exception
	{
		return MATH.sqrt(arg);
	}

    @VDMFunction
	public static Value pi_f() throws Exception
	{
		return MATH.pi_f();
	}

    @VDMOperation
	public static Value rand(Value arg) throws ValueException
	{
		return MATH.rand(arg);
	}

    @VDMOperation
	public static Value srand2(Value arg) throws ValueException
	{
		return MATH.srand2(arg);
	}

    @VDMFunction
	public static Value exp(Value arg) throws ValueException, Exception
	{
		return MATH.exp(arg);
	}

    @VDMFunction
	public static Value ln(Value arg) throws ValueException, Exception
	{
		return MATH.ln(arg);
	}

    @VDMFunction
	public static Value log(Value arg) throws ValueException, Exception
	{
		return MATH.log(arg);
	}

    @VDMFunction
	public static Value fac(Value arg) throws ValueException, Exception
	{
		return MATH.fac(arg);
	}

	//
	// VDMUtil...
	//

    @VDMFunction
	public static Value set2seq(Value arg) throws ValueException
	{
		return VDMUtil.set2seq(arg);
	}

    @VDMFunction
	public static Value val2seq_of_char(Value arg)
	{
		return VDMUtil.val2seq_of_char(arg);
	}

    @VDMFunction
	public static Value seq_of_char2val_(Value arg)
	{
		return VDMUtil.seq_of_char2val_(arg);
	}
}
