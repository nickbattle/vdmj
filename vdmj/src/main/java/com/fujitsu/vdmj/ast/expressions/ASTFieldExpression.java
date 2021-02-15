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

package com.fujitsu.vdmj.ast.expressions;

import com.fujitsu.vdmj.ast.expressions.visitors.ASTExpressionVisitor;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;

public class ASTFieldExpression extends ASTExpression
{
	private static final long serialVersionUID = 1L;
	public final ASTExpression object;
	public final LexIdentifierToken field;
	public final LexNameToken memberName;

	public ASTFieldExpression(ASTExpression object, LexIdentifierToken field)
	{
		super(object);
		this.object = object;
		this.field = field;
		this.memberName = null;
	}

	public ASTFieldExpression(ASTExpression object, LexNameToken field)
	{
		super(object);
		this.object = object;
		this.field = new LexIdentifierToken(field.name, field.old, field.location);
		this.memberName = field;
	}

	@Override
	public String toString()
	{
		return "(" + object + "." +
			(memberName == null ? field.name : memberName.getName()) + ")";
	}

	@Override
	public String kind()
	{
		return "field name";
	}

	@Override
	public <R, S> R apply(ASTExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFieldExpression(this, arg);
	}
}
