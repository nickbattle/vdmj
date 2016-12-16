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
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

abstract public class TCNumericBinaryExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCNumericBinaryExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	abstract public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint);

	protected void checkNumeric(Environment env, NameScope scope)
	{
		ltype = left.typeCheck(env, null, scope, null);
		rtype = right.typeCheck(env, null, scope, null);

		if (!ltype.isNumeric(location))
		{
			report(3139, "Left hand of " + op + " is not numeric");
			detail("Actual", ltype);
			ltype = new TCRealType(location);
		}

		if (!rtype.isNumeric(location))
		{
			report(3140, "Right hand of " + op + " is not numeric");
			detail("Actual", rtype);
			rtype = new TCRealType(location);
		}
	}
}
