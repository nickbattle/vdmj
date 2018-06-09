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

import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTraceAnnotation implements TCExpressionAnnotation
{
	public final TCIdentifierToken name;
	
	public final TCExpressionList args;

	public TCTraceAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		this.name = name;
		this.args = args;
	}
	
	@Override
	public String toString()
	{
		return "@" + name + "(" + args + ")";
	}

	@Override
	public void typeCheck(Environment env, NameScope scope)
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
