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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSameBaseClassExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression left;
	public final TCExpression right;

	public TCSameBaseClassExpression(LexLocation start, TCExpression left, TCExpression right)
	{
		super(start);

		this.left = left;
		this.right = right;
	}
	@Override
	public String toString()
	{
		return "samebaseclass(" + left + "," + right + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType lt = left.typeCheck(env, null, scope, null);

		if (!lt.isClass(env))
		{
			left.report(3266, "Argument is not an object");
		}

		TCType rt = right.typeCheck(env, null, scope, null);

		if (!rt.isClass(env))
		{
			right.report(3266, "Argument is not an object");
		}

		return checkConstraint(constraint, new TCBooleanType(location));
	}
}
