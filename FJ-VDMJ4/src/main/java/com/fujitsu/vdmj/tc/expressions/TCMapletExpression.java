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

import java.io.Serializable;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCMapletExpression implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final TCExpression left;
	public final TCExpression right;

	public TCMapletExpression(TCExpression left, TCExpression right)
	{
		this.location = left.location;
		this.left = left;
		this.right = right;
	}

	public TCType typeCheck(Environment env, NameScope scope, TCType domConstraint, TCType rngConstraint)
	{
		TCType ltype = left.typeCheck(env, null, scope, domConstraint);
		TCType rtype = right.typeCheck(env, null, scope, rngConstraint);

		return new TCMapType(location, ltype, rtype);
	}

	@Override
	public String toString()
	{
		return left + " |-> " + right;
	}

	public TCNameSet getFreeVariables(Environment env)
	{
		TCNameSet names = left.getFreeVariables(env);
		names.addAll(right.getFreeVariables(env));
		return names;
	}
}
