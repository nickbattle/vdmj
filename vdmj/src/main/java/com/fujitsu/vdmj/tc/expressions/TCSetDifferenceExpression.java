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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCSetDifferenceExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCSetDifferenceExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ltype = left.typeCheck(env, null, scope, null);
		rtype = right.typeCheck(env, null, scope, null);

		if (!ltype.isSet(location))
		{
			report(3160, "Left hand of '\\' is not a set");
		}

		if (!rtype.isSet(location))
		{
			report(3161, "Right hand of '\\' is not a set");
		}

		if (!TypeComparator.compatible(ltype, rtype))
		{
			report(3162, "Left and right of '\\' are different types");
			detail2("Left", ltype, "Right", rtype);
		}

		if (ltype instanceof TCSet1Type)
		{
			TCSet1Type set1 = (TCSet1Type)ltype;
			return setType(new TCSetType(location, set1.setof));
		}
		else
		{
			return setType(ltype);
		}
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetDifferenceExpression(this, arg);
	}
}
