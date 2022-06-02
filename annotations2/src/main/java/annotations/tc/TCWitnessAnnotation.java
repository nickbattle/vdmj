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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package annotations.tc;

import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCWitnessAnnotation extends TCAnnotation
{
	private static final long serialVersionUID = 1L;
	private static TCDefinitionList tagDefinitions = null;
	private TCValueDefinition myDefinition = null;

	public TCWitnessAnnotation(TCIdentifierToken name, TCExpressionList args)
	{
		super(name, args);
	}
	
	public static void doInit()
	{
		tagDefinitions = new TCDefinitionList();
		
		for (TCAnnotation a: getInstances(TCWitnessAnnotation.class))
		{
			TCWitnessAnnotation witness = (TCWitnessAnnotation)a;
			
			if (witness.args.get(0) instanceof TCVariableExpression &&
				witness.args.get(1) instanceof TCExpression)
			{
				tagDefinitions.add(witness.createDefinition());
			}
		}
	}
	
	private TCValueDefinition createDefinition()
	{
		TCVariableExpression tag = (TCVariableExpression)args.get(0);
		myDefinition  = new TCValueDefinition(NameScope.ANYTHING, null,
				null, new TCIdentifierPattern(tag.name), null, args.get(1));
		
		return myDefinition;
	}

	@Override
	protected void doInit(Environment globals)
	{
		Environment local = new FlatEnvironment(tagDefinitions, globals);
		tagDefinitions.typeCheck(local, NameScope.ANYTHING);
	}

	@Override
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		Environment local = new FlatEnvironment(tagDefinitions, env);
		
		args.get(0).typeCheck(local, null, scope, null);
		args.get(1).typeCheck(local, null, scope, null);
	}
}
