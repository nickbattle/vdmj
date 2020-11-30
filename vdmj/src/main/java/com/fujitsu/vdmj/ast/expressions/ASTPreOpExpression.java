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

package com.fujitsu.vdmj.ast.expressions;

import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.ast.statements.ASTErrorCaseList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class ASTPreOpExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final ASTExpression expression;
	public final ASTErrorCaseList errors;
	public final ASTStateDefinition state;

	public ASTPreOpExpression(
		TCNameToken opname, ASTExpression expression, ASTErrorCaseList errors, ASTStateDefinition state)
	{
		super(expression);
		this.opname = opname;
		this.expression = expression;
		this.errors = errors;
		this.state = state;
	}

	@Override
	public String toString()
	{
		return expression.toString();
	}

	@Override
	public String kind()
	{
		return "pre_op";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePreOpExpression(this, arg);
	}
}
