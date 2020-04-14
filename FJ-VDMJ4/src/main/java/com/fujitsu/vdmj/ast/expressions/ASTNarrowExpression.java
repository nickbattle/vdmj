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

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTNarrowExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final ASTType basictype;
	public final LexNameToken typename;
	public final ASTExpression test;

	public ASTNarrowExpression(LexLocation location, LexNameToken typename, ASTExpression test)
	{
		super(location);
		this.basictype = null;
		this.typename = typename;
		this.test = test;
	}

	public ASTNarrowExpression(LexLocation location, ASTType type, ASTExpression test)
	{
		super(location);
		this.basictype = type;
		this.typename = null;
		this.test = test;
	}

	@Override
	public String toString()
	{
		return "narrow_(" + test + ", " + (typename == null ? basictype : typename) + ")";
	}

	@Override
	public String kind()
	{
		return "narrow_";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNarrowExpression(this, arg);
	}
}
