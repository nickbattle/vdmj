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

import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.statements.ASTErrorCaseList;

public class ASTPostOpExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken opname;
	public final ASTExpression preexpression;
	public final ASTExpression postexpression;
	public final ASTErrorCaseList errors;
	public final ASTStateDefinition state;

	public ASTPostOpExpression(
		LexNameToken opname, ASTExpression preexpression, ASTExpression postexpression,
		ASTErrorCaseList errors, ASTStateDefinition state)
	{
		super(postexpression.location);
		this.opname = opname;
		this.preexpression = preexpression;
		this.postexpression = postexpression;
		this.errors = errors;
		this.state = state;
	}

	@Override
	public String toString()
	{
		return postexpression.toString();
	}

	@Override
	public String kind()
	{
		return "post_op";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePostOpExpression(this, arg);
	}
}
