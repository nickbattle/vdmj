/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCSystemDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCHistoryExpression;
import com.fujitsu.vdmj.tc.expressions.TCIntegerLiteralExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;

public class TCSeparateAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	
	private Environment globals = null;

	public TCSeparateAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}
	
	@Override
	protected void doInit(Environment globals)
	{
		this.globals = globals;
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		name.report(6009, "@Separate only applies to system classes");
	}

	@Override
	public void tcBefore(TCModule module)
	{
		name.report(6009, "@Separate only applies to system classes");
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		if (clazz instanceof TCSystemDefinition)
		{
			checkArgs(new PrivateClassEnvironment(clazz, globals), NameScope.ANYTHING);
		}
		else
		{
			name.report(6009, "@Separate only applies to system classes");
		}
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(6009, "@Separate only applies to system classes");
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(6009, "@Separate only applies to system classes");
	}

	private void checkArgs(Environment env, NameScope scope)
	{
		if (args.size() != 3)
		{
			name.report(6008, "Expecting @Separate(<start>, <end>, <time>)");
		}
		else
		{
			if (!(args.get(0) instanceof TCHistoryExpression))
			{
				args.get(0).report(6008, "Expecting history expression (eg. #fin(op))");
			}
			
			if (!(args.get(1) instanceof TCHistoryExpression))
			{
				args.get(1).report(6008, "Expecting history expression (eg. #req(op))");
			}
			
			if (!(args.get(2) instanceof TCIntegerLiteralExpression))
			{
				args.get(2).report(6008, "Expecting integer literal expression");
			}

			args.get(0).typeCheck(env, null, scope, null);
			args.get(1).typeCheck(env, null, scope, null);
			TCType time = args.get(2).typeCheck(env, null, scope, null);
			
			if (!time.isNumeric(LexLocation.ANY))
			{
				args.get(2).report(6008, "Expecting integer expression, got " + time);
			}
		}
	}
}
