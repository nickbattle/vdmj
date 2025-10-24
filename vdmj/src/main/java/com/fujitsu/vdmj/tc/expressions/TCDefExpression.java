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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCDefExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCDefinitionList localDefs;
	public final TCExpression expression;

	public TCDefExpression(LexLocation location,
		TCDefinitionList localDefs, TCExpression expression)
	{
		super(location);
		this.localDefs = localDefs;
		this.expression = expression;
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		// Each local definition is in scope for later local definitions...

		Environment local = env;

		for (TCDefinition d: localDefs)
		{
			d.implicitDefinitions(local);
			d.typeResolve(local);
			d.typeCheck(local, scope);

			/***
			if (Settings.dialect == Dialect.VDM_SL && d instanceof TCEqualsDefinition)
			{
				TCEqualsDefinition eqdef = (TCEqualsDefinition)d;

				if (eqdef.test instanceof TCApplyExpression)
				{
					// Simple op call or a function call, so fine
				}
				else if (eqdef.test.callsOperations(env))
				{
					// Complex expression that calls an operation
					eqdef.test.warning(9999, "RHS of 'def' should be an op call or a pure expression");
				}
			}
			***/

			local = new FlatCheckedEnvironment(d, local, scope);	// cumulative
		}

		TCType r = expression.typeCheck(local, null, scope, constraint);
		local.unusedCheck(env);
		return setType(r);
	}

	@Override
	public String toString()
	{
		return "def " + Utils.listToString(localDefs) + " in\n" + expression;
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDefExpression(this, arg);
	}
}
