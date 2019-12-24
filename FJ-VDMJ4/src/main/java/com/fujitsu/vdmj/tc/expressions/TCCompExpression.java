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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCCompExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCCompExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ltype = left.typeCheck(env, null, scope, null);
		rtype = right.typeCheck(env, null, scope, null);
		TCTypeSet results = new TCTypeSet();

		if (ltype.isMap(location))
		{
    		if (!rtype.isMap(location))
    		{
    			report(3068, "Right hand of map 'comp' is not a map");
    			detail("Type", rtype);
    			return new TCMapType(location);	// Unknown types
    		}

    		TCMapType lm = ltype.getMap();
    		TCMapType rm = rtype.getMap();

    		if (!TypeComparator.compatible(lm.from, rm.to))
    		{
    			report(3069, "Domain of left should equal range of right in map 'comp'");
    			detail2("Dom", lm.from, "Rng", rm.to);
    		}

    		results.add(new TCMapType(location, rm.from, lm.to));
		}

		if (ltype.isFunction(location))
		{
    		if (!rtype.isFunction(location))
    		{
    			report(3070, "Right hand of function 'comp' is not a function");
    			detail("Type", rtype);
    			return new TCUnknownType(location);
    		}
    		else
    		{
        		TCFunctionType lf = ltype.getFunction();
        		TCFunctionType rf = rtype.getFunction();

        		if (lf.parameters.size() != 1)
        		{
        			report(3071, "Left hand function must have a single parameter");
        			detail("Type", lf);
        		}
        		else if (rf.parameters.size() != 1)
        		{
        			report(3072, "Right hand function must have a single parameter");
        			detail("Type", rf);
        		}
        		else if (!TypeComparator.compatible(lf.parameters.get(0), rf.result))
        		{
        			report(3073, "Parameter of left should equal result of right in function 'comp'");
        			detail2("Parameter", lf.parameters.get(0), "Result", rf.result);
        		}

        		results.add(new TCFunctionType(location, rf.parameters, true, lf.result));
    		}
		}

		if (results.isEmpty())
		{
			report(3074, "Left hand of 'comp' is neither a map nor a function");
			detail("Type", ltype);
			return new TCUnknownType(location);
		}

		return results.getType(location);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCompExpression(this, arg);
	}
}
