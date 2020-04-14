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

import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTExists1Expression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final ASTBind bind;
	public final ASTExpression predicate;

	public ASTExists1Expression(LexLocation location, ASTBind bind, ASTExpression predicate)
	{
		super(location);
		this.bind = bind;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "(exists1 " + bind + " & " + predicate + ")";
	}

	@Override
	public String kind()
	{
		return "exists1";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExists1Expression(this, arg);
	}
}
