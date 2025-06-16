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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.expressions;

import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTIfExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression ifExp;
	public final ASTExpression thenExp;
	public final ASTElseIfExpressionList elseList;
	public final ASTExpression elseExp;

	public ASTIfExpression(LexLocation location,
		ASTExpression ifExp, ASTExpression thenExp, ASTElseIfExpressionList elseList,
		ASTExpression elseExp)
	{
		super(location);
		this.ifExp = ifExp;
		this.thenExp = thenExp;
		this.elseList = elseList;
		this.elseExp = elseExp;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(if " + ifExp + "\nthen " + thenExp);

		for (ASTElseIfExpression s: elseList)
		{
			sb.append("\n");
			sb.append(s.toString());
		}

		if (elseExp != null)
		{
			sb.append("\nelse ");
			sb.append(elseExp.toString());
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public String kind()
	{
		return "if";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIfExpression(this, arg);
	}
}
