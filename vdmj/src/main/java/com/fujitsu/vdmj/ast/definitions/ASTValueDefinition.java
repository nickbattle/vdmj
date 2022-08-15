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

package com.fujitsu.vdmj.ast.definitions;

import com.fujitsu.vdmj.ast.definitions.visitors.ASTDefinitionVisitor;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class to hold a value definition.
 */
public class ASTValueDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final NameScope scope;
	public final ASTPattern pattern;
	public final ASTType type;
	public ASTExpression exp;

	public ASTValueDefinition(NameScope scope, ASTPattern p, ASTType type, ASTExpression exp)
	{
		super(p.location, null);

		this.scope = scope;
		this.pattern = p;
		this.type = type;
		this.exp = exp;
	}

	@Override
	public void setAccessSpecifier(ASTAccessSpecifier access)
	{
		if (access == null)
		{
			access = new ASTAccessSpecifier(true, false, Token.PRIVATE, false);
		}
		else if (!access.isStatic)
		{
			access = new ASTAccessSpecifier(true, false, access.access, false);
		}

		super.setAccessSpecifier(access);
	}

	@Override
	public String toString()
	{
		return accessSpecifier.ifSet(" ") + pattern +
				(type == null ? "" : ":" + type) + " = " + exp;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTValueDefinition)
		{
			return other.toString().equals(toString());
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public String kind()
	{
		return "value";
	}

	@Override
	public <R, S> R apply(ASTDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseValueDefinition(this, arg);
	}

	public void setExpression(ASTExpression newExpression)
	{
		this.exp = newExpression;
	}
}
