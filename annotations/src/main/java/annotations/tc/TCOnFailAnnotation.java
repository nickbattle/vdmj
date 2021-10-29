/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

package annotations.tc;

import java.util.Arrays;

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCOnFailAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

	public TCOnFailAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		name.report(3359, "@OnFail only applies to expressions");
	}

	@Override
	public void tcBefore(TCModule module)
	{
		name.report(3359, "@OnFail only applies to expressions");
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		name.report(3359, "@OnFail only applies to expressions");
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(3359, "@OnFail only applies to expressions");
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		if (args.isEmpty())
		{
			name.report(6008, "@OnFail([errno,] \"format\", args...)");
		}
		else
		{
			if (args.get(0) instanceof TCStringLiteralExpression ||
				args.get(0) instanceof TCIntegerLiteralExpression &&
				args.size() > 1 &&
				args.get(1) instanceof TCStringLiteralExpression)
			{
				for (TCExpression arg: args)
				{
					arg.typeCheck(env, null, scope, null);	// Just checks scope
				}
				
				TCStringLiteralExpression str = null;
				
				if (args.get(0) instanceof TCStringLiteralExpression)
				{
					str = (TCStringLiteralExpression)args.get(0);
				}
				else
				{
					str = (TCStringLiteralExpression)args.get(1);
				}
				
				String format = str.value.value;
				
				try
				{
					// Try to format with string arguments to check they are all %s (up to 20)
					Object[] args = new String[20];
					Arrays.fill(args, "A string");
					String.format(format, args);
				}
				catch (IllegalArgumentException e)
				{
					name.report(6008, "@OnFail must only use %[arg$][width]s conversions");
				}
			}
			else
			{
				name.report(6008, "@OnFail([errno,] \"format\", args...)");
			}
		}
	}
	
	@Override
	public void tcAfter(TCExpression exp, TCType type, Environment env, NameScope scope)
	{
		if (!(type instanceof TCBooleanType))
		{
			name.report(3361, "@OnFail not applied to boolean expression");
		}
	}
}
