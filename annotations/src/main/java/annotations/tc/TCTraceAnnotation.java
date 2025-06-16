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

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTraceAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

	public TCTraceAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		name.report(6006, "@Trace only applies to expressions and statements");
	}

	@Override
	public void tcBefore(TCModule module)
	{
		name.report(6006, "@Trace only applies to expressions and statements");
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		name.report(6006, "@Trace only applies to expressions and statements");
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		check(env, scope);
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		check(env, scope);
	}
	
	private void check(Environment env, NameScope scope)
	{
		for (TCExpression arg: args)
		{
			if (!(arg instanceof TCVariableExpression))
			{
				arg.report(6007, "@Trace argument must be an identifier");
			}
			else
			{
				arg.typeCheck(env, null, scope, null);	// Just checks scope
			}
		}
	}
}
