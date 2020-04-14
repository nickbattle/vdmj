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

import com.fujitsu.vdmj.lex.LexLocation;

public class ASTSetRangeExpression extends ASTSetExpression
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression first;
	public final ASTExpression last;

	public ASTSetRangeExpression(LexLocation start, ASTExpression first, ASTExpression last)
	{
		super(start);
		this.first = first;
		this.last = last;
	}

	@Override
	public String toString()
	{
		return "{" + first + ", ... ," + last + "}";
	}

	@Override
	public String kind()
	{
		return "set range";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetRangeExpression(this, arg);
	}
}
