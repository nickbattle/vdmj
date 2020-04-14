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

public class ASTSubseqExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression seq;
	public final ASTExpression from;
	public final ASTExpression to;

	public ASTSubseqExpression(ASTExpression seq, ASTExpression from, ASTExpression to)
	{
		super(seq);
		this.seq = seq;
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return "(" + seq + "(" + from + ", ... ," + to + "))";
	}

	@Override
	public String kind()
	{
		return "subsequence";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSubseqExpression(this, arg);
	}
}
