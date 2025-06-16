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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.definitions;

import com.fujitsu.vdmj.ast.definitions.visitors.ASTDefinitionVisitor;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.patterns.ASTBind;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTTypeBind;
import com.fujitsu.vdmj.lex.LexLocation;

/**
 * A class to hold an equals definition.
 */
public class ASTEqualsDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTPattern pattern;
	public final ASTTypeBind typebind;
	public final ASTBind bind;
	public final ASTExpression test;

	public ASTEqualsDefinition(LexLocation location, ASTPattern pattern, ASTExpression test)
	{
		super(location, null);
		this.pattern = pattern;
		this.typebind = null;
		this.bind = null;
		this.test = test;
	}

	public ASTEqualsDefinition(LexLocation location, ASTTypeBind typebind, ASTExpression test)
	{
		super(location, null);
		this.pattern = null;
		this.typebind = typebind;
		this.bind = null;
		this.test = test;
	}

	public ASTEqualsDefinition(LexLocation location, ASTBind setbind, ASTExpression test)
	{
		super(location, null);
		this.pattern = null;
		this.typebind = null;
		this.bind = setbind;
		this.test = test;
	}

	@Override
	public String toString()
	{
		return (pattern != null ? pattern :
				typebind != null ? typebind : bind) + " = " + test;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTEqualsDefinition)
		{
			return toString().equals(other.toString());
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
		return "equals";
	}

	@Override
	public <R, S> R apply(ASTDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseEqualsDefinition(this, arg);
	}
}
