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

package com.fujitsu.vdmj.ast.expressions;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTAnnotatedExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;

	public final LexIdentifierToken name;
	
	public final ASTExpressionList args;

	public final ASTExpression expression;
	
	public ASTAnnotatedExpression(LexLocation location, LexIdentifierToken name, ASTExpressionList args, ASTExpression expression)
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
	public String kind()
	{
		return "annotated expression";
	}
}
