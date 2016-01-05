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

import org.apfloat.Apfloat;
import org.apfloat.ApfloatMath;
import org.overturetool.vdmj.Settings;
import org.overturetool.vdmj.lex.LexToken;
import org.overturetool.vdmj.pog.FuncIterationObligation;
import org.overturetool.vdmj.pog.MapIterationObligation;
import org.overturetool.vdmj.pog.POContextStack;
import org.overturetool.vdmj.pog.ProofObligationList;
import org.overturetool.vdmj.runtime.Context;
import org.overturetool.vdmj.runtime.ValueException;
import org.overturetool.vdmj.typechecker.Environment;
import org.overturetool.vdmj.typechecker.NameScope;
import org.overturetool.vdmj.types.Type;
import org.overturetool.vdmj.types.TypeList;
import org.overturetool.vdmj.types.UnknownType;
import org.overturetool.vdmj.values.FunctionValue;
import org.overturetool.vdmj.values.IterFunctionValue;
import org.overturetool.vdmj.values.MapValue;
import org.overturetool.vdmj.values.NumericValue;
import org.overturetool.vdmj.values.Value;
import org.overturetool.vdmj.values.ValueMap;

public class StarStarExpression extends BinaryExpression
{
	private static final long serialVersionUID = 1L;

	private static final BigInteger BIG_MAX_INT = new BigInteger(Integer.toString(Integer.MAX_VALUE));
	
	public StarStarExpression(Expression left, LexToken op, Expression right)
	{
		super(left, op, right);
	}

	@Override
	public Type typeCheck(Environment env, TypeList qualifiers, NameScope scope, Type constraint)
	{
		ltype = left.typeCheck(env, null, scope, null);
		rtype = right.typeCheck(env, null, scope, null);

		if (ltype.isMap())
		{
			if (!rtype.isNumeric())
			{
				rtype.report(3170, "Map iterator expects nat as right hand arg");
			}
		}
		else if (ltype.isFunction())
		{
			if (!rtype.isNumeric())
			{
				rtype.report(3171, "Function iterator expects nat as right hand arg");
			}
		}
		else if (ltype.isNumeric())
		{
			if (!rtype.isNumeric())
			{
				rtype.report(3172, "'**' expects number as right hand arg");
			}
		}
		else
		{
			report(3173, "First arg of '**' must be a map, function or number");
			return new UnknownType(location);
		}

		return ltype;
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		try
		{
    		Value lv = left.eval(ctxt).deref();
    		Value rv = right.eval(ctxt);

    		if (lv instanceof MapValue)
    		{
    			ValueMap map = lv.mapValue(ctxt);
    			long n = rv.intValue(ctxt).longValue();
    			ValueMap result = new ValueMap();

    			for (Value k: map.keySet())
    			{
    				Value r = k;

    				for (int i=0; i<n; i++)
    				{
    					r = map.get(r);
    				}

    				if (r == null)
    				{
						abort(4133, "Map range is not a subset of its domain: " + k, ctxt);
    				}

					Value old = result.put(k, r);

					if (old != null && !old.equals(r))
					{
						abort(4030, "Duplicate map keys have different values: " + k, ctxt);
					}
				}

    			return new MapValue(result);
    		}
    		else if (lv instanceof FunctionValue)
    		{
    			return new IterFunctionValue(
    				lv.functionValue(ctxt), rv.intValue(ctxt).longValue());
    		}
    		else if (lv instanceof NumericValue)
    		{
    			if (NumericValue.areIntegers(lv, rv))
    			{
    				if (rv.intValue(ctxt).compareTo(BIG_MAX_INT) < 0)
    				{
    					return NumericValue.valueOf(lv.intValue(ctxt).pow(rv.intValue(ctxt).intValue()), ctxt);
    				}
    			}

    			Apfloat ld = new Apfloat(lv.realValue(ctxt));
    			Apfloat rd = new Apfloat(rv.realValue(ctxt));
    			Apfloat result = ApfloatMath.pow(ld, rd);

    			return NumericValue.valueOf(new BigDecimal(result.toString(), Settings.precision), ctxt);
    		}

    		return abort(4031,
    			"First arg of '**' must be a map, function or number", ctxt);
 		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();

		if (ltype.isFunction())
		{
			String prename = left.getPreName();

			if (prename == null || !prename.equals(""))
			{
				obligations.add(
					new FuncIterationObligation(this, prename, ctxt));
			}
		}

		if (ltype.isMap())
		{
			obligations.add(new MapIterationObligation(this, ctxt));
		}

		return obligations;
	}

	@Override
	public String kind()
	{
		return "**";
	}
}
