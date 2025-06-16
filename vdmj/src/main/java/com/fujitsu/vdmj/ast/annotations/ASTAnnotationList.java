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

import java.util.Vector;

import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.syntax.StatementReader;
import com.fujitsu.vdmj.syntax.SyntaxReader;

public class ASTAnnotationList extends Vector<ASTAnnotation> implements Mappable
{
	private static final long serialVersionUID = 1L;

	public void astBefore(SyntaxReader reader)
	{
		for (ASTAnnotation annotation: this)
		{
			if (reader instanceof DefinitionReader)
			{
				annotation.astBefore((DefinitionReader)reader);
			}
			else if (reader instanceof ExpressionReader)
			{
				annotation.astBefore((ExpressionReader)reader);
			}
			else if (reader instanceof StatementReader)
			{
				annotation.astBefore((StatementReader)reader);
			}
			else if (reader instanceof ModuleReader)
			{
				annotation.astBefore((ModuleReader)reader);
			}
			else if (reader instanceof ClassReader)
			{
				annotation.astBefore((ClassReader)reader);
			}
		}
	}

	public void astAfter(DefinitionReader reader, ASTDefinition def)
	{
		for (ASTAnnotation annotation: this)
		{
			annotation.astAfter(reader, def);
		}
	}

	public void astAfter(StatementReader reader, ASTStatement stmt)
	{
		for (ASTAnnotation annotation: this)
		{
			annotation.astAfter(reader, stmt);
		}
	}

	public void astAfter(ExpressionReader reader, ASTExpression exp)
	{
		for (ASTAnnotation annotation: this)
		{
			annotation.astAfter(reader, exp);
		}
	}

	public void astAfter(ModuleReader reader, ASTModule module)
	{
		for (ASTAnnotation annotation: this)
		{
			annotation.astAfter(reader, module);
		}
	}

	public void astAfter(ClassReader reader, ASTClassDefinition clazz)
	{
		for (ASTAnnotation annotation: this)
		{
			annotation.astAfter(reader, clazz);
		}
	}

	public boolean isBracketed()
	{
		for (ASTAnnotation annotation: this)
		{
			if (annotation.isBracketed())
			{
				return true;
			}
		}

		return false;
	}
}
