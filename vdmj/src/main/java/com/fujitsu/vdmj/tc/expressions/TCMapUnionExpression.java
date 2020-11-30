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
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCMapUnionExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCMapUnionExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ltype = left.typeCheck(env, null, scope, constraint);
		rtype = right.typeCheck(env, null, scope, constraint);

		if (!ltype.isMap(location))
		{
			report(3123, "Left hand of 'munion' is not a map");
			detail("Type", ltype);
			return new TCMapType(location);	// Unknown types
		}
		else if (!rtype.isMap(location))
		{
			report(3124, "Right hand of 'munion' is not a map");
			detail("Type", rtype);
			return ltype;
		}
		else
		{
			TCMapType ml = ltype.getMap();
			TCMapType mr = rtype.getMap();

			TCTypeSet from = new TCTypeSet(ml.from, mr.from);
			TCTypeSet to = new TCTypeSet(ml.to, mr.to);

			return new TCMapType(location,
				from.getType(location), to.getType(location));
		}
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapUnionExpression(this, arg);
	}
}
