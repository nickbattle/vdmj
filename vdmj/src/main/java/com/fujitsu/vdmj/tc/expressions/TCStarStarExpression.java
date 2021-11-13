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
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCStarStarExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCStarStarExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		ltype = left.typeCheck(env, null, scope, null);
		rtype = right.typeCheck(env, null, scope, null);

		if (ltype.isMap(location))
		{
			if (!rtype.isNumeric(location))
			{
				right.report(3170, "Map iterator expects nat as right hand arg");
			}
		}
		else if (ltype.isFunction(location))
		{
			if (!rtype.isNumeric(location))
			{
				right.report(3171, "Function iterator expects nat as right hand arg");
			}
		}
		else if (ltype.isNumeric(location))
		{
			if (!rtype.isNumeric(location))
			{
				right.report(3172, "'**' expects number as right hand arg");
			}
		}
		else
		{
			report(3173, "First arg of '**' must be a map, function or number");
			return setType(new TCUnknownType(location));
		}

		return setType(ltype);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStarStarExpression(this, arg);
	}
}
