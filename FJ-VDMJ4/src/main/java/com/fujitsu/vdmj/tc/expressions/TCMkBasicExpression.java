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

import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCMkBasicExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCType type;
	public final TCExpression arg;

	public TCMkBasicExpression(TCType type, TCExpression arg)
	{
		super(type.location);
		this.type = type;
		this.arg = arg;
	}

	@Override
	public String toString()
	{
		return "mk_" + type + "(" + arg + ")";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType argtype = arg.typeCheck(env, null, scope, null);

		if (!(type instanceof TCTokenType) && !argtype.equals(type))
		{
			report(3125, "Argument of mk_" + type + " is the wrong type");
		}

		return checkConstraint(constraint, type);
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		TCNameSet names = type.getFreeVariables(env);
		names.addAll(arg.getFreeVariables(globals, env));
		return names;
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMkBasicExpression(this, arg);
	}
}
