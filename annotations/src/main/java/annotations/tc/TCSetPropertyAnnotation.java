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

package annotations.tc;

import java.lang.reflect.Field;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSetPropertyAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

	public TCSetPropertyAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		before();
	}
	
	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		before();
	}
	
	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		before();
	}

	@Override
	public void tcBefore(TCModule m)
	{
		before();
	}

	@Override
	public void tcBefore(TCClassDefinition m)
	{
		before();
	}

	private void before()
	{
		if (args.size() != 2 ||
			!(args.get(0) instanceof TCStringLiteralExpression))
		{
			name.report(7500, "Expecting @setProperty(\"vdmj.<area>.<name>\", <value>)");
		}
		else
		{
			Class<Properties> pclass = Properties.class;
			
			// Names are all "vdmj.<area>.some_name" for field <area>_some_name.
			TCStringLiteralExpression name = (TCStringLiteralExpression)args.get(0);
			String fname = name.value.value.replace("vdmj.", "");
			fname = fname.replace(".", "_");

			TCExpression fvalue = args.get(1);

			try
			{
				Field field = pclass.getField(fname);

				if (field.getType().equals(int.class) &&
					!(fvalue instanceof TCIntegerLiteralExpression))
				{
					name.report(7501, "@setProperty(" + name + ", value) expects an integer value");
				}
				else if (field.getType().equals(boolean.class) &&
					!(fvalue instanceof TCBooleanLiteralExpression))
				{
					name.report(7502, "@setProperty(" + name + ", value) expects a boolean value");
				}
				else if (field.getType().equals(String.class) &&
					!(fvalue instanceof TCStringLiteralExpression))
				{
					name.report(7503, "@setProperty(" + name + ", value) expects a string value");
				}
				else
				{
					name.report(7504, "@setProperty unexpected field type: " + field.getType());
				}
			}
			catch (Exception e)
			{
				name.report(7505, "@setProperty throws " + e);
			}
		}
	}
}
