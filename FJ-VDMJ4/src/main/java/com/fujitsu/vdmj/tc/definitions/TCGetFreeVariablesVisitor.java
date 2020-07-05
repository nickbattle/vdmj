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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.expressions.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCGetFreeVariablesVisitor extends TCLeafDefinitionVisitor<TCNameToken, TCNameSet, EnvTriple>
{
	private TCExpressionVisitor<TCNameSet, EnvTriple> expVisitor =
		new com.fujitsu.vdmj.tc.expressions.TCGetFreeVariablesVisitor(this);
	
	private TCStatementVisitor<TCNameSet, EnvTriple> stmtVisitor =
		new com.fujitsu.vdmj.tc.statements.TCGetFreeVariablesVisitor(this);

	private TCTypeVisitor<TCNameSet, EnvTriple> typeVisitor =
		new com.fujitsu.vdmj.tc.types.TCGetFreeVariablesVisitor(this);
	
	protected TCGetFreeVariablesVisitor()
	{
		super(null);
	}

	protected TCGetFreeVariablesVisitor(TCVisitorSet<TCNameToken, TCNameSet, EnvTriple> visitors)
	{
		super(visitors);
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCExpressionVisitor<TCNameSet, EnvTriple> getExpressionVisitor()
	{
		return (visitorSet == null) ? expVisitor : visitorSet.getExpressionVisitor();
	}

	@Override
	public TCStatementVisitor<TCNameSet, EnvTriple> getStatementVisitor()
	{
		return (visitorSet == null) ? stmtVisitor : visitorSet.getStatementVisitor();
	}

	@Override
	public TCTypeVisitor<TCNameSet, EnvTriple> getTypeVisitor()
	{
		return (visitorSet == null) ? typeVisitor : visitorSet.getTypeVisitor();
	}

	@Override
	public TCNameSet caseDefinition(TCDefinition node, EnvTriple arg)
	{
		return newCollection();
	}

	@Override
	public TCNameSet caseEqualsDefinition(TCEqualsDefinition node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node.defs, arg.env);
		return node.test.apply(expVisitor, new EnvTriple(arg.globals, local, arg.returns));
	}
	
	@Override
	public TCNameSet caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, EnvTriple arg)
	{
		TCDefinitionList defs = new TCDefinitionList();
		
		if (node.paramDefinitionList != null)
		{
    		for (TCDefinitionList pdef: node.paramDefinitionList)
    		{
    			defs.addAll(pdef);	// All definitions of all parameter lists
    		}
		}

		Environment local = new FlatEnvironment(defs, arg.env);
		TCNameSet names = node.body.apply(expVisitor, new EnvTriple(arg.globals, local, arg.returns));
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseExplicitOperationDefinition(TCExplicitOperationDefinition node, EnvTriple arg)
	{
		TCDefinitionList defs = new TCDefinitionList();
		
		if (node.paramDefinitions != null)
		{
			defs.addAll(node.paramDefinitions);
		}

		Environment local = new FlatEnvironment(defs, arg.env);
		TCNameSet names = node.body.apply(stmtVisitor, new EnvTriple(arg.globals, local, arg.returns));
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, EnvTriple arg)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCPatternListTypePair pltp: node.parameterPatterns)
		{
			defs.addAll(pltp.getDefinitions(NameScope.LOCAL));
		}

		Environment local = new FlatEnvironment(defs, arg.env);
		TCNameSet names = new TCNameSet();
		
		if (node.body != null)
		{
			names.addAll(node.body.apply(expVisitor, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseImplicitOperationDefinition(TCImplicitOperationDefinition node, EnvTriple arg)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCPatternListTypePair pltp: node.parameterPatterns)
		{
			defs.addAll(pltp.getDefinitions(NameScope.LOCAL));
		}

		Environment local = new FlatEnvironment(defs, arg.env);
		TCNameSet names = new TCNameSet();
		
		if (node.body != null)
		{
			names.addAll(node.body.apply(stmtVisitor, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseInstanceVariableDefinition(TCInstanceVariableDefinition node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		names.addAll(node.type.apply(typeVisitor, arg));
		names.addAll(node.expression.apply(expVisitor, arg));
		return names;
	}
	
	@Override
	public TCNameSet caseLocalDefinition(TCLocalDefinition node, EnvTriple arg)
	{
		TCNameSet names = node.type.apply(typeVisitor, arg);
		
		if (node.valueDefinition != null)
		{
			names.addAll(node.valueDefinition.apply(this, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseStateDefinition(TCStateDefinition node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node, arg.env);
		TCNameSet names = new TCNameSet();
		
		if (node.invdef != null)
		{
			names.addAll(node.invdef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		if (node.initdef != null)
		{
			names.addAll(node.initdef.apply(this, new EnvTriple(arg.globals, local, arg.returns)));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseTypeDefinition(TCTypeDefinition node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		
		if (node.type instanceof TCNamedType)
		{
			TCNamedType nt = (TCNamedType)node.type;
			names.addAll(nt.type.apply(typeVisitor, arg));
		}
		
		if (node.invdef != null)
		{
			names.addAll(node.invdef.apply(this, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseValueDefinition(TCValueDefinition node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		
		if (node.type != null)
		{
			names.addAll(node.type.apply(typeVisitor, arg));
		}
		
		names.addAll(node.exp.apply(expVisitor, arg));
		return names;
	}
}
