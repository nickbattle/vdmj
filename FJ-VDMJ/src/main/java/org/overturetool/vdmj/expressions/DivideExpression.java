/*******************************************************************************
 *
 *	Copyright (c) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.expressions;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.overturetool.vdmj.Settings;
import org.overturetool.vdmj.lex.LexToken;
import org.overturetool.vdmj.pog.NonZeroObligation;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.RealType;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.values.NumericValue;
import org.overturetool.vdmj.values.Value;

public class DivideExpression extends NumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public DivideExpression(Expression left, LexToken op, Expression right)
	{
		super(left, op, right);
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		checkNumeric(env, scope);
		return new RealType(location);
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		try
		{
    		Value l = left.eval(ctxt);
    		Value r = right.eval(ctxt);

			if (NumericValue.areIntegers(l, r))
			{
				BigInteger lv = l.intValue(ctxt);
				BigInteger rv = r.intValue(ctxt);
				BigInteger[] result = lv.divideAndRemainder(rv);
				
				if (result[1].equals(BigInteger.ZERO))	// Else do BigDecimal
				{
					return NumericValue.valueOf(result[0], ctxt);
				}
			}

			BigDecimal lv = l.realValue(ctxt);
    		BigDecimal rv = r.realValue(ctxt);

    		return NumericValue.valueOf(lv.divide(rv, Settings.precision), ctxt);
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
		catch (Exception e)
		{
			return abort(new ValueException(4134, e.getMessage(), ctxt));
		}
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = super.getProofObligations(ctxt);

		if (!(right instanceof IntegerLiteralExpression) &&
			!(right instanceof RealLiteralExpression))
		{
			obligations.add(new NonZeroObligation(location, right, ctxt));
		}

		return obligations;
	}

	@Override
	public String kind()
	{
		return "divide";
	}
}
