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

package com.fujitsu.vdmj.ast.definitions;

import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.lex.Token;

public class ASTThreadDefinition extends ASTDefinition
{
	private static final long serialVersionUID = 1L;
	public final ASTStatement statement;

	public ASTThreadDefinition(ASTStatement statement)
	{
		super(statement.location, null);
		this.statement = statement;
		setAccessSpecifier(new ASTAccessSpecifier(false, false, Token.PROTECTED, false));
	}

	@Override
	public String kind()
	{
		return "thread";
	}

	@Override
	public String toString()
	{
		return "thread " + statement.toString();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTThreadDefinition)
		{
			ASTThreadDefinition tho = (ASTThreadDefinition)other;
			return tho.statement.equals(statement);
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return statement.hashCode();
	}

	@Override
	public <R, S> R apply(ASTDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseThreadDefinition(this, arg);
	}
}
