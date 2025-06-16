/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.ast.annotations;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.MappingOptional;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.syntax.StatementReader;

public abstract class ASTAnnotation extends ASTNode implements MappingOptional
{
	private static final long serialVersionUID = 1L;
	public final LexIdentifierToken name;
	public ASTExpressionList args = null;

	public ASTAnnotation(LexIdentifierToken name)
	{
		this.name = name;
	}
	
	@Override
	public String toString()
	{
		return "@" + name + (args == null || args.isEmpty() ? "" : "(" + args + ")");
	}
	
	protected void parseException(String message, LexLocation location) throws LexException
	{
		throw new LexException(0, "Malformed @" + name.name + ": " + message, location);
	}
	
	/**
	 * The default parse for annotations looks for an optional list of expressions in
	 * round brackets. This method can be overridden in particular annotations if the
	 * default syntax is not appropriate. 
	 */
	public void parse(LexTokenReader ltr) throws LexException, ParserException
	{
		this.args = new ASTExpressionList();

		if (ltr.nextToken().is(Token.BRA))
		{
			if (ltr.nextToken().isNot(Token.KET))
			{
				ExpressionReader er = new ExpressionReader(ltr);
				args.add(er.readExpression());
		
				while (ltr.getLast().is(Token.COMMA))
				{
					ltr.nextToken();
					args.add(er.readExpression());
				}
			}
	
			if (ltr.getLast().isNot(Token.KET))
			{
				parseException("Expecting ')' after annotation", ltr.getLast().location);
			}
		}
	}

	public void astBefore(DefinitionReader reader)
	{
		// Nothing by default
	}

	public void astBefore(StatementReader reader)
	{
		// Nothing by default
	}

	public void astBefore(ExpressionReader reader)
	{
		// Nothing by default
	}

	public void astBefore(ModuleReader reader)
	{
		// Nothing by default
	}

	public void astBefore(ClassReader reader)
	{
		// Nothing by default
	}

	public void astAfter(DefinitionReader reader, ASTDefinition def)
	{
		// Nothing by default
	}

	public void astAfter(StatementReader reader, ASTStatement stmt)
	{
		// Nothing by default
	}

	public void astAfter(ExpressionReader reader, ASTExpression exp)
	{
		// Nothing by default
	}

	public void astAfter(ModuleReader reader, ASTModule module)
	{
		// Nothing by default
	}

	public void astAfter(ClassReader reader, ASTClassDefinition clazz)
	{
		// Nothing by default
	}
	
	public boolean isBracketed()
	{
		return false;
	}
}
