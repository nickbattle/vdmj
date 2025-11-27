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

package com.fujitsu.vdmj.tc.annotations;

import java.util.Iterator;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCOperationMeasureAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;

	public TCOperationMeasureAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		if (!def.isOperation())
		{
			name.report(6006, "@OperationMeasure only applies to operations");
		}
		else if (args.size() != 1)
		{
			name.report(6007, "@OperationMeasure arg: <numeric expression>");
		}
		else
		{
			Environment params = getParamEnvironment(def, env);
			TCType type = args.firstElement().typeCheck(params, null, scope, null);

			if (type.isNumeric(type.location))
			{
				TCNumericType ntype = type.getNumeric();

				if (ntype.getWeight() > 2)		// NOT nat1, nat or int => rat or real
				{
					name.report(6007, "@OperationMeasure argument must be type nat");
					name.detail("Actual", ntype.toString());
				}
			}
			else
			{
				name.report(6007, "@OperationMeasure argument must be numeric");
				name.detail("Actual", type.toString());
			}
		}
	}

	private Environment getParamEnvironment(TCDefinition def, Environment env)
	{
		TCDefinitionList defs = new TCDefinitionList();

		if (def instanceof TCExplicitOperationDefinition)
		{
			TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition)def;
			Iterator<TCType> titer = exop.type.parameters.iterator();

			for (TCPattern p: exop.parameterPatterns)
			{
				TCType ptype = titer.next();
				defs.addAll(p.getDefinitions(ptype, ptype.isClass(env) ? NameScope.STATE : NameScope.LOCAL));
			}
		}
		else if (def instanceof TCImplicitOperationDefinition)
		{
			TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)def;
			TCDefinitionList paramDefinitions = new TCDefinitionList();

			for (TCPatternListTypePair ptp: imop.parameterPatterns)
			{
				paramDefinitions.addAll(ptp.getDefinitions(ptp.type.isClass(env) ? NameScope.STATE : NameScope.LOCAL));
			}
		}

		return new FlatEnvironment(defs, env);
	}

	@Override
	public void tcBefore(TCModule module)
	{
		name.report(6006, "@OperationMeasure only applies to operations");
	}

	@Override
	public void tcBefore(TCClassDefinition clazz)
	{
		name.report(6006, "@OperationMeasure only applies to operations");
	}

	@Override
	public void tcBefore(TCExpression exp, Environment env, NameScope scope)
	{
		name.report(6006, "@OperationMeasure only applies to operations");
	}

	@Override
	public void tcBefore(TCStatement stmt, Environment env, NameScope scope)
	{
		name.report(6006, "@OperationMeasure only applies to operations");
	}
}
