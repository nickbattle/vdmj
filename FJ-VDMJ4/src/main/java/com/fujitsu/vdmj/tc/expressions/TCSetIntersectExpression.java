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
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCSetIntersectExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCSetIntersectExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ltype = left.typeCheck(env, null, scope, null);
		rtype = right.typeCheck(env, null, scope, null);
		
		TCType lset = null;
		TCType rset = null;

		if (!ltype.isSet(location))
		{
			report(3163, "Left hand of " + location + " is not a set");
		}
		else
		{
			lset = ltype.getSet().setof;
		}

		if (!rtype.isSet(location))
		{
			report(3164, "Right hand of " + location + " is not a set");
		}
		else
		{
			rset = rtype.getSet().setof;
		}
		
		TCType result = ltype;	// A guess
		
		if (lset != null && !lset.isUnknown(location) && rset != null && !rset.isUnknown(location))
		{
			TCType interTypes = TypeComparator.intersect(lset, rset);
	
			if (interTypes == null)
			{
				report(3165, "Left and right of intersect are different types");
				detail2("Left", ltype, "Right", rtype);
			}
			else
			{
				result = new TCSetType(location, interTypes);
			}
		}

		return result;
	}
}
