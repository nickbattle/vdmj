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
import com.fujitsu.vdmj.tc.annotations.TCAnnotatedExpression;
import com.fujitsu.vdmj.tc.annotations.TCAnnotatedStatement;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCCasesStatement;
import com.fujitsu.vdmj.tc.statements.TCExitStatement;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForIndexStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCIfStatement;
import com.fujitsu.vdmj.tc.statements.TCLetBeStStatement;
import com.fujitsu.vdmj.tc.statements.TCLetDefStatement;
import com.fujitsu.vdmj.tc.statements.TCReturnStatement;
import com.fujitsu.vdmj.tc.statements.TCSimpleBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCSpecificationStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCTixeStatement;
import com.fujitsu.vdmj.tc.statements.TCTrapStatement;
import com.fujitsu.vdmj.tc.statements.TCWhileStatement;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;

public class TCDependencyStatementVisitor extends TCLeafStatementVisitor<TCNameToken, TCNameSet, EnvTriple>
{
	public TCDependencyStatementVisitor(TCVisitorSet<TCNameToken, TCNameSet, EnvTriple> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, EnvTriple arg)
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseAnnotatedStatement(TCAnnotatedStatement node, EnvTriple arg)
	{
		return newCollection();		// Don't search annotations for dependencies
	}

	@Override
	public TCNameSet caseBlockStatement(TCBlockStatement node, EnvTriple arg)
	{
		Environment local = arg.env;

		for (TCDefinition d: node.assignmentDefs)
		{
			local = new FlatEnvironment(d, local);	// cumulative
		}

		return caseSimpleBlockStatement(node, new EnvTriple(arg.globals, local, arg.returns));
	}
	
	@Override
	public TCNameSet caseCallStatement(TCCallStatement node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet(node.name.getExplicit(true));
		
		for (TCExpression a: node.args)
		{
			names.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseCasesStatement(TCCasesStatement node, EnvTriple arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);	// Cases are conditional
	}
	
	@Override
	public TCNameSet caseExitStatement(TCExitStatement node, EnvTriple arg)
	{
		arg.returns.set(true);

		if (node.expression == null)
		{
			return new TCNameSet();
		}
		
		return visitorSet.applyExpressionVisitor(node.expression, arg);
	}
	
	@Override
	public TCNameSet caseForAllStatement(TCForAllStatement node, EnvTriple arg)
	{
		return visitorSet.applyExpressionVisitor(node.set, arg);
	}
	
	@Override
	public TCNameSet caseForIndexStatement(TCForIndexStatement node, EnvTriple arg)
	{
		TCNameSet names = visitorSet.applyExpressionVisitor(node.from, arg);
		names.addAll(visitorSet.applyExpressionVisitor(node.to, arg));
		
		if (node.by != null)
		{
			names.addAll(visitorSet.applyExpressionVisitor(node.by, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseForPatternBindStatement(TCForPatternBindStatement node, EnvTriple arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
	}
	
	@Override
	public TCNameSet caseIfStatement(TCIfStatement node, EnvTriple arg)
	{
		return visitorSet.applyExpressionVisitor(node.ifExp, arg);
	}
	
	@Override
	public TCNameSet caseLetBeStStatement(TCLetBeStStatement node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = visitorSet.applyMultiBindVisitor(node.bind, new EnvTriple(arg.globals, local, arg.returns));
		
		if (node.suchThat != null)
		{
			names.addAll(visitorSet.applyExpressionVisitor(node.suchThat, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		names.addAll(node.statement.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		return names;
	}
	
	@Override
	public TCNameSet caseLetDefStatement(TCLetDefStatement node, EnvTriple arg)
	{
		Environment local = arg.env;
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
				names.addAll(visitorSet.applyDefinitionVisitor(d,
						new EnvTriple(arg.globals, local, arg.returns)));
			}
		}

		names.addAll(node.statement.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		return names;
	}
	
	@Override
	public TCNameSet caseReturnStatement(TCReturnStatement node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		
		if (node.expression != null)
		{
			names.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
		}
		
		arg.returns.set(true);		// So everything that follows is conditional
		return names;
	}
	
	@Override
	public TCNameSet caseSimpleBlockStatement(TCSimpleBlockStatement node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		
		for (TCStatement stmt: node.statements)
		{
    		if (!arg.returns.get())
    		{
    			names.addAll(stmt.apply(this, arg));
    		}
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseSpecificationStatement(TCSpecificationStatement node, EnvTriple arg)
	{
		return newCollection();
	}
	
	@Override
	public TCNameSet caseTixeStatement(TCTixeStatement node, EnvTriple arg)
	{
		return node.body.apply(this, arg);
	}
	
	@Override
	public TCNameSet caseTrapStatement(TCTrapStatement node, EnvTriple arg)
	{
		return node.body.apply(this, arg);
	}
	
	@Override
	public TCNameSet caseWhileStatement(TCWhileStatement node, EnvTriple arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
	}
}
