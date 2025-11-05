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
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURTCSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.statements.visitors;

import com.fujitsu.vdmj.tc.annotations.TCAnnotatedStatement;
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCAssignmentStatement;
import com.fujitsu.vdmj.tc.statements.TCBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A visitor set to explore the TC tree and return the state names updated by
 * local assignments within the statement(s). Note that this skips over op calls.
 */
public class TCLocalUpdatesVisitor extends TCLeafStatementVisitor<TCNameToken, TCNameSet, FlatEnvironment>
{
	public TCLocalUpdatesVisitor()
	{
		super();
	}

	@Override
	public TCNameSet caseAnnotatedStatement(TCAnnotatedStatement node, FlatEnvironment locals)
	{
		return node.statement.apply(this, locals);	// Don't process args
	}
	
	@Override
	public TCNameSet caseAssignmentStatement(TCAssignmentStatement node, FlatEnvironment locals)
	{
		TCNameToken name = node.target.updatedVariableName();
		TCNameSet result = newCollection();

		if (locals == null || locals.findName(name, NameScope.STATE) == null)
		{
			result.add(name);
		}

		return result;
	}

	@Override
	public TCNameSet caseBlockStatement(TCBlockStatement node, FlatEnvironment locals)
	{
		TCNameSet result = newCollection();
		FlatEnvironment block = new FlatEnvironment(new TCDefinitionList(), locals);

		for (TCDefinition def: node.assignmentDefs)
		{
			if (def instanceof TCAssignmentDefinition)
			{
				TCAssignmentDefinition adef = (TCAssignmentDefinition)def;
				block.add(adef);
			}
		}

		for (TCStatement statement: node.statements)
		{
			result.addAll(statement.apply(this, block));
		}

		return result;
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, FlatEnvironment locals)
	{
		return newCollection();
	}
}
