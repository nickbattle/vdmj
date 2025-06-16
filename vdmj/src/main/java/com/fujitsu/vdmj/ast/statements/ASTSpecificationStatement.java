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

package com.fujitsu.vdmj.ast.statements;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTSpecificationStatement extends ASTStatement
{
	private static final long serialVersionUID = 1L;
	public final ASTExternalClauseList externals;
	public final ASTExpression precondition;
	public final ASTExpression postcondition;
	public final ASTErrorCaseList errors;

	public ASTSpecificationStatement(LexLocation location,
		ASTExternalClauseList externals, ASTExpression precondition,
		ASTExpression postcondition, ASTErrorCaseList errors)
	{
		super(location);

		this.externals = externals;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;
	}

	@Override
	public String toString()
	{
		return "[" +
    		(externals == null ? "" : "\n\text " + externals) +
    		(precondition == null ? "" : "\n\tpre " + precondition) +
    		(postcondition == null ? "" : "\n\tpost " + postcondition) +
    		(errors == null ? "" : "\n\terrs " + errors) + "]";
	}

	@Override
	public String kind()
	{
		return "specification";
	}

	@Override
	public <R, S> R apply(ASTStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSpecificationStatement(this, arg);
	}
}
