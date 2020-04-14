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

package com.fujitsu.vdmj.ast.statements;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTCasesStatement extends ASTStatement
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression exp;
	public final ASTCaseStmtAlternativeList cases;
	public final ASTStatement others;

	public ASTCasesStatement(LexLocation location,
		ASTExpression exp, ASTCaseStmtAlternativeList cases, ASTStatement others)
	{
		super(location);
		this.exp = exp;
		this.cases = cases;
		this.others = others;
	}

	@Override
	public String kind()
	{
		return "cases";
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("cases " + exp + " :\n");

		for (ASTCaseStmtAlternative csa: cases)
		{
			sb.append("  ");
			sb.append(csa.toString());
		}

		if (others != null)
		{
			sb.append("  others -> ");
			sb.append(others.toString());
		}

		sb.append("esac");
		return sb.toString();
	}

	@Override
	public <R, S> R apply(ASTStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesStatement(this, arg);
	}
}
