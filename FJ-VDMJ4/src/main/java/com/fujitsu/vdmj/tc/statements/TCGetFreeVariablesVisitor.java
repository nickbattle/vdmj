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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLeafDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;

public class TCGetFreeVariablesVisitor extends TCLeafStatementVisitor<TCNameToken, TCNameSet, EnvTriple>
{
	private TCLeafExpressionVisitor<TCNameToken, TCNameSet, EnvTriple> expVisitor =
		new com.fujitsu.vdmj.tc.expressions.TCGetFreeVariablesVisitor();

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	protected TCLeafExpressionVisitor<TCNameToken, TCNameSet, EnvTriple> getExpressionVisitor()
	{
		return expVisitor;
	}

	@Override
	protected TCLeafDefinitionVisitor<TCNameToken, TCNameSet, EnvTriple> getDefinitionVisitor()
	{
		return null;
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, EnvTriple arg)
	{
		return new TCNameSet();
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
			names.addAll(a.apply(expVisitor, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseCasesStatement(TCCasesStatement node, EnvTriple arg)
	{
		return node.exp.apply(expVisitor, arg);		// Cases are conditional
	}
	
	@Override
	public TCNameSet caseExitStatement(TCExitStatement node, EnvTriple arg)
	{
		arg.returns.set(true);

		if (node.expression == null)
		{
			return new TCNameSet();
		}
		
		return node.expression.apply(expVisitor, arg);
	}
	
	@Override
	public TCNameSet caseForAllStatement(TCForAllStatement node, EnvTriple arg)
	{
		return node.set.apply(expVisitor, arg);
	}
	
	@Override
	public TCNameSet caseForIndexStatement(TCForIndexStatement node, EnvTriple arg)
	{
		TCNameSet names = node.from.apply(expVisitor, arg);
		names.addAll(node.to.apply(expVisitor, arg));
		
		if (node.by != null)
		{
			names.addAll(node.by.apply(expVisitor, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseForPatternBindStatement(TCForPatternBindStatement node, EnvTriple arg)
	{
		return node.exp.apply(expVisitor, arg);
	}
	
	@Override
	public TCNameSet caseIfStatement(TCIfStatement node, EnvTriple arg)
	{
		return node.ifExp.apply(expVisitor, arg);
	}
	
	@Override
	public TCNameSet caseLetBeStStatement(TCLetBeStStatement node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.def, arg.env);
		TCNameSet names = node.bind.getFreeVariables(arg.globals, local);
		
		if (node.suchThat != null)
		{
			names.addAll(node.suchThat.apply(expVisitor, new EnvTriple(arg.globals, local, arg.returns)));
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
				names.addAll(d.getFreeVariables(arg.globals, local, arg.returns));
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
			names.addAll(node.expression.apply(expVisitor, arg));
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
		return node.exp.apply(expVisitor, arg);
	}
}
