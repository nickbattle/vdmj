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
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSetUnionExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCSetUnionExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ltype = left.typeCheck(env, null, scope, constraint);
		rtype = right.typeCheck(env, null, scope, constraint);

		if (!ltype.isSet(location))
		{
			report(3168, "Left hand of " + op + " is not a set");
			ltype = new TCSetType(location, new TCUnknownType(location));
		}

		if (!rtype.isSet(location))
		{
			report(3169, "Right hand of " + op + " is not a set");
			rtype = new TCSetType(location, new TCUnknownType(location));
		}

		TCType lof = ltype.getSet();
		TCType rof = rtype.getSet();
		boolean set1 = (lof instanceof TCSet1Type) || (rof instanceof TCSet1Type);
		
		lof = ((TCSetType)lof).setof;
		rof = ((TCSetType)rof).setof;
		TCTypeSet ts = new TCTypeSet(lof, rof);
		
		return set1 ?
			new TCSet1Type(location, ts.getType(location)) :
			new TCSetType(location, ts.getType(location));
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetUnionExpression(this, arg);
	}
}
