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

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTCaseStmtAlternative extends ASTNode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final ASTExpression exp;
	public final ASTPattern pattern;
	public final ASTStatement statement;

	public ASTCaseStmtAlternative(ASTExpression exp, ASTPattern pattern, ASTStatement stmt)
	{
		this.location = pattern.location;
		this.exp = exp;
		this.pattern = pattern;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "case " + pattern + " -> " + statement;
	}
}
