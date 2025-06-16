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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeChecker;

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
		Environment local = new FlatCheckedEnvironment(tagDefinitions, globals, NameScope.GLOBAL);
		List<VDMError> errs = TypeChecker.getErrors();
		int before = errs.size();
		myDefinition.typeCheck(local, NameScope.ANYTHING);
		
		if (errs.size() > before)
		{
			List<VDMError> problems = new Vector<VDMError>();
			int after = errs.size();
			
			for (int i = before; i < after; i++)
			{
				problems.add(errs.remove(before));	// Always remove this one
			}

			TypeChecker.report(6666, "Bad witness", name.getLocation());

			for (VDMError e: problems)
			{
				TypeChecker.detail("Witness", e);
			}
		}
	}
}
