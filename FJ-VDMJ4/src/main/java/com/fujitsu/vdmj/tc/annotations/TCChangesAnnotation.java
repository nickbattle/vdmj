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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.annotations;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCStringLiteralExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCChangesAnnotation extends TCAnnotation
{
	public TCChangesAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void typeCheck(TCDefinition def, Environment env, NameScope scope)
	{
		name.report(3359, "@Changes only applies to statements");
	}

	@Override
	public void typeCheck(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(3359, "@Changes only applies to statements");
	}

	@Override
	public void typeCheck(TCStatement stmt, Environment env, NameScope scope)
	{
		if (args.size() == 1)
		{
			if (!(args.get(0) instanceof TCStringLiteralExpression))
			{
				name.report(3361, "@Changes argument must be a string literal");
			}
		}
		else if (args.size() > 1)
		{
			name.report(3361, "@Changes has one optional string argument");
		}
	}
}
