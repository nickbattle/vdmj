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
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTSameBaseClassExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression left;
	public final ASTExpression right;

	public ASTSameBaseClassExpression(LexLocation start, ASTExpressionList args)
	{
		super(start);
		left = args.get(0);
		right = args.get(1);
	}

	@Override
	public String kind()
	{
		return "samebaseclass";
	}

	@Override
	public String toString()
	{
		return "samebaseclass(" + left + "," + right + ")";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSameBaseClassExpression(this, arg);
	}
}
