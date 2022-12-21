/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.tc.statements.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCCaseStmtAlternative;
import com.fujitsu.vdmj.tc.statements.TCCasesStatement;
import com.fujitsu.vdmj.tc.statements.TCLetBeStStatement;
import com.fujitsu.vdmj.tc.statements.TCLetDefStatement;
import com.fujitsu.vdmj.tc.statements.TCSpecificationStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCFreeVariableStatementVisitor extends TCLeafStatementVisitor<TCNameToken, TCNameSet, Environment>
{
	public TCFreeVariableStatementVisitor(TCVisitorSet<TCNameToken, TCNameSet, Environment> visitors)
	{
		assert visitors != null : "Visitor set cannot be null";
		visitorSet = visitors;
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, Environment arg)
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseBlockStatement(TCBlockStatement node, Environment arg)
	{
		Environment local = arg;

		for (TCDefinition d: node.assignmentDefs)
		{
			local = new FlatEnvironment(d, local);	// cumulative
		}

		return caseSimpleBlockStatement(node, local);
	}
	
 	@Override
	public TCNameSet caseCasesStatement(TCCasesStatement node, Environment arg)
	{
 		TCNameSet all = visitorSet.applyExpressionVisitor(node.exp, arg);
		
		for (TCCaseStmtAlternative a: node.cases)
		{
			Environment local = new FlatEnvironment(a.pattern.getDefinitions(node.expType, NameScope.LOCAL), arg);
			all.addAll(visitorSet.applyPatternVisitor(a.pattern, local));
			all.addAll(a.statement.apply(this, local));
		}
		
		all.addAll(visitorSet.applyStatementVisitor(node.others, arg));
		return all;
	}
	
	@Override
	public TCNameSet caseLetBeStStatement(TCLetBeStStatement node, Environment arg)
	{
		TCNameSet names = visitorSet.applyMultiBindVisitor(node.bind, arg);
		Environment local = new FlatEnvironment(node.def, arg);
		names.addAll(visitorSet.applyExpressionVisitor(node.suchThat, local));
		names.addAll(node.statement.apply(this, local));
		return names;
	}
	
	@Override
	public TCNameSet caseLetDefStatement(TCLetDefStatement node, Environment arg)
	{
		Environment local = arg;
		TCNameSet names = new TCNameSet();

		for (TCDefinition d: node.localDefs)
		{
			if (d instanceof TCExplicitFunctionDefinition)
			{
				// ignore
			}
			else
			{
				local = new FlatEnvironment(d, local);
				names.addAll(visitorSet.applyDefinitionVisitor(d, local));
			}
		}

		names.addAll(node.statement.apply(this, local));
		return names;
	}
	
	@Override
	public TCNameSet caseSpecificationStatement(TCSpecificationStatement node, Environment arg)
	{
		return newCollection();
	}
}
