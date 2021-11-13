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
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTimesExpression extends TCNumericBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCTimesExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		checkNumeric(env, scope);

		TCNumericType ln = ltype.getNumeric();
		TCNumericType rn = rtype.getNumeric();

		if (ln instanceof TCRealType)
		{
			return setType(ln);
		}
		else if (rn instanceof TCRealType)
		{
			return setType(rn);
		}
		else if (ln instanceof TCIntegerType)
		{
			return setType(ln);
		}
		else if (rn instanceof TCIntegerType)
		{
			return setType(rn);
		}
		else if (ln instanceof TCNaturalType)
		{
			return setType(ln);
		}
		else if (rn instanceof TCNaturalType)
		{
			return setType(rn);
		}
		else
		{
			return setType(new TCNaturalOneType(ln.location));
		}
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTimesExpression(this, arg);
	}
}
