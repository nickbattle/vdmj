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

import java.lang.reflect.Field;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.expressions.ASTBooleanLiteralExpression;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.expressions.ASTIntegerLiteralExpression;
import com.fujitsu.vdmj.ast.expressions.ASTStringLiteralExpression;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.syntax.ClassReader;
import com.fujitsu.vdmj.syntax.DefinitionReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.syntax.StatementReader;
import com.fujitsu.vdmj.syntax.SyntaxReader;

public class ASTSetPropertyAnnotation extends ASTAnnotation
{
	private static final long serialVersionUID = 1L;

	public ASTSetPropertyAnnotation(LexIdentifierToken name)
	{
		super(name);
	}

	@Override
	public void astBefore(DefinitionReader reader)
	{
		before(reader);
	}

	@Override
	public void astBefore(StatementReader reader)
	{
		before(reader);
	}

	@Override
	public void astBefore(ExpressionReader reader)
	{
		before(reader);
	}

	@Override
	public void astBefore(ModuleReader reader)
	{
		before(reader);
	}

	@Override
	public void astBefore(ClassReader reader)
	{
		before(reader);
	}

	private void before(SyntaxReader reader)	// @SetProperty("name", value)
	{
		if (args.size() == 2 &&
			args.get(0) instanceof ASTStringLiteralExpression)
		{
			Class<Properties> pclass = Properties.class;
			
			// Names are all "vdmj.<area>.some_name" being <area>_some_name.
			ASTStringLiteralExpression sname = (ASTStringLiteralExpression)args.get(0);
			String fname = sname.value.value.replace("vdmj.", "");
			fname = fname.replace(".", "_");

			ASTExpression fvalue = args.get(1);

			try
			{
				// Set both the Properties field and the System properties, to simulate
				// calling Properties.init for one field.
				Field field = pclass.getField(fname);

				if (field.getType().equals(int.class))
				{
					if (fvalue instanceof ASTIntegerLiteralExpression)
					{
						ASTIntegerLiteralExpression exp = (ASTIntegerLiteralExpression)fvalue;
						field.set(null, (int)exp.value.value);
						System.setProperty(sname.value.value, exp.toString());
					}
					else
					{
						reader.report(7500, "Expecting integer", fvalue.location);
					}
				}
				else if (field.getType().equals(boolean.class))
				{
					if (fvalue instanceof ASTBooleanLiteralExpression)
					{
						ASTBooleanLiteralExpression exp = (ASTBooleanLiteralExpression)fvalue;
						field.set(null, exp.value.value);
						System.setProperty(sname.value.value, exp.toString());
					}
					else
					{
						reader.report(7501, "Expecting boolean", fvalue.location);
					}
				}
				else if (field.getType().equals(String.class))
				{
					if (fvalue instanceof ASTStringLiteralExpression)
					{
						ASTStringLiteralExpression exp = (ASTStringLiteralExpression)fvalue;
						field.set(null, exp.value.value);
						System.setProperty(sname.value.value, exp.value.value);
					}
					else
					{
						reader.report(7502, "Expecting String", fvalue.location);
					}
				}
				else
				{
					reader.report(7503, "Unknown field type " + field.getType(), name.location);
				}
			}
			catch (NoSuchFieldException e)
			{
				reader.report(7504, "Unknown VDMJ property " + sname, sname.location);
			}
			catch (Exception e)
			{
				reader.report(7505, "@SetProperty throws " + e, name.location);
			}
		}
		else
		{
			reader.report(7506, "Expecting @SetProperty(\"vdmj.<area>.<name>\", <value>)", name.location);
		}
	}
}
