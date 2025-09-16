/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package annotations.ast;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.syntax.StatementReader;

public class ASTMaximalTypesAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;

	public ASTMaximalTypesAnnotation(LexIdentifierToken name)
	{
		super(name);
	}

	private boolean saved = false;

	@Override
	public void astBefore(DefinitionReader reader)
	{
		before();
	}

	@Override
	public void astBefore(StatementReader reader)
	{
		before();
	}

	@Override
	public void astBefore(ExpressionReader reader)
	{
		before();
	}

	@Override
	public void astBefore(ModuleReader reader)
	{
		before();
	}

	@Override
	public void astBefore(ClassReader reader)
	{
		before();
	}

	private void before()
	{
		saved = Properties.parser_maximal_types;
		Properties.parser_maximal_types = true;
	}

	@Override
	public void astAfter(DefinitionReader reader, ASTDefinition def)
	{
		after();
	}

	@Override
	public void astAfter(StatementReader reader, ASTStatement stmt)
	{
		after();
	}

	@Override
	public void astAfter(ExpressionReader reader, ASTExpression exp)
	{
		after();
	}

	@Override
	public void astAfter(ModuleReader reader, ASTModule module)
	{
		after();
	}

	@Override
	public void astAfter(ClassReader reader, ASTClassDefinition clazz)
	{
		after();
	}

	private void after()
	{
		Properties.parser_maximal_types = saved;
		saved = false;
	}

	@Override
	public boolean isBracketed()
	{
		return true;
	}
}
