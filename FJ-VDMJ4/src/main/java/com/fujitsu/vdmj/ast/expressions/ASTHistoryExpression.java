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

import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.ast.lex.LexNameList;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;

public class ASTHistoryExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final Token hop;
	public final LexNameList opnames;

	public ASTHistoryExpression(LexLocation location, Token hop, LexNameList opnames)
	{
		super(location);
		this.hop = hop;
		this.opnames = opnames;
	}

	@Override
	public String kind()
	{
		return toString();
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(hop.toString().toLowerCase());
		sb.append("(");
		String sep = "";

		for (LexNameToken opname: opnames)
		{
			sb.append(sep);
			sep = ", ";
			sb.append(opname.name);
		}

		sb.append(")");
		return sb.toString();
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseHistoryExpression(this, arg);
	}
}
