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

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.util.Utils;

public class ASTCallStatement extends ASTStatement
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken name;
	public final ASTExpressionList args;

	public ASTCallStatement(LexNameToken name, ASTExpressionList args)
	{
		super(name.location);
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return name.name + "(" + Utils.listToString(args) + ")";
	}

	@Override
	public String kind()
	{
		return "op call";
	}

	@Override
	public <R, S> R apply(ASTStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCallStatement(this, arg);
	}
}
