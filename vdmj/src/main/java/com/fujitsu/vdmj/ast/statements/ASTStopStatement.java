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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.statements;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.statements.visitors.ASTStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTStopStatement extends ASTStatement
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression objects;

	public ASTStopStatement(LexLocation location, ASTExpression obj)
	{
		super(location);
		this.objects = obj;
	}

	@Override
	public String kind()
	{
		return "stop";
	}

	@Override
	public String toString()
	{
		return kind() + "(" + objects + ")";
	}

	@Override
	public <R, S> R apply(ASTStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStopStatement(this, arg);
	}
}
