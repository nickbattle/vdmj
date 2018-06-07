/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCAnnotatedExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken name;
	
	public final TCExpressionList args;

	public final TCExpression expression;
	
	public TCAnnotatedExpression(LexLocation location, TCIdentifierToken name, TCExpressionList args, TCExpression expression)
	{
		super(location);
		this.name = name;
		this.args = args;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "@" + name + "(" + args + ") " + expression;
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		if (name.getName().equals("Trace"))
		{
			checkTrace(env, scope);
		}
		else
		{
			name.report(3357, "Unknown annotation @" + name);
		}
		
		return expression.typeCheck(env, qualifiers, scope, constraint);
	}

	private void checkTrace(Environment env, NameScope scope)
	{
		for (TCExpression arg: args)
		{
			if (!(arg instanceof TCVariableExpression))
			{
				arg.report(3358, "@Trace argument must be an identifier");
			}
			else
			{
				arg.typeCheck(env, null, scope, null);	// Just checks scope
			}
		}
	}
}
