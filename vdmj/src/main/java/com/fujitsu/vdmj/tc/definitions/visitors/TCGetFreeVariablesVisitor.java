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

package com.fujitsu.vdmj.tc.definitions.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.EnvTriple;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCGetFreeVariablesBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCGetFreeVariablesMultipleBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindVisitor;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCGetFreeVariablesVisitor extends TCLeafDefinitionVisitor<TCNameToken, TCNameSet, EnvTriple>
{
	private static class VisitorSet extends TCVisitorSet<TCNameToken, TCNameSet, EnvTriple>
	{
		private final TCGetFreeVariablesVisitor defVisitor;
		private final TCExpressionVisitor<TCNameSet, EnvTriple> expVisitor;
		private final TCStatementVisitor<TCNameSet, EnvTriple> stmtVisitor;
		private final TCTypeVisitor<TCNameSet, EnvTriple> typeVisitor;
		private final TCBindVisitor<TCNameSet, EnvTriple> bindVisitor;
		private final TCMultipleBindVisitor<TCNameSet, EnvTriple> mbindVisitor;

		public VisitorSet(TCGetFreeVariablesVisitor parent)
		{
			defVisitor = parent;
			expVisitor = new com.fujitsu.vdmj.tc.expressions.visitors.TCGetFreeVariablesVisitor(this);
			stmtVisitor = new com.fujitsu.vdmj.tc.statements.visitors.TCGetFreeVariablesVisitor(this);
			typeVisitor = new com.fujitsu.vdmj.tc.types.visitors.TCGetFreeVariablesVisitor(this);
			bindVisitor = new TCGetFreeVariablesBindVisitor(this);
			mbindVisitor = new TCGetFreeVariablesMultipleBindVisitor(this); 
		}
		
		@Override
		public TCDefinitionVisitor<TCNameSet, EnvTriple> getDefinitionVisitor()
		{
			return defVisitor;
		}

		@Override
		public TCExpressionVisitor<TCNameSet, EnvTriple> getExpressionVisitor()
	 	{
	 		return expVisitor;
	 	}
	 	
		@Override
		public TCStatementVisitor<TCNameSet, EnvTriple> getStatementVisitor()
	 	{
	 		return stmtVisitor;
	 	}
	 	
		@Override
		public TCTypeVisitor<TCNameSet, EnvTriple> getTypeVisitor()
	 	{
	 		return typeVisitor;
	 	}
		
		@Override
		public TCBindVisitor<TCNameSet, EnvTriple> getBindVisitor()
		{
			return bindVisitor;
		}
		
		@Override
		public TCMultipleBindVisitor<TCNameSet, EnvTriple> getMultiBindVisitor()
		{
			return mbindVisitor;
		}
	}

	public TCGetFreeVariablesVisitor()
	{
		visitorSet = new VisitorSet(this);
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
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
		return node.test.apply(visitorSet.getExpressionVisitor(), new EnvTriple(arg.globals, local, arg.returns));
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
		TCNameSet names = node.body.apply(visitorSet.getExpressionVisitor(), new EnvTriple(arg.globals, local, arg.returns));
		
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
		TCNameSet names = node.body.apply(visitorSet.getStatementVisitor(), new EnvTriple(arg.globals, local, arg.returns));
		
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
			names.addAll(node.body.apply(visitorSet.getExpressionVisitor(), new EnvTriple(arg.globals, local, arg.returns)));
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
			names.addAll(node.body.apply(visitorSet.getStatementVisitor(), new EnvTriple(arg.globals, local, arg.returns)));
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
		names.addAll(node.type.apply(visitorSet.getTypeVisitor(), arg));
		names.addAll(node.expression.apply(visitorSet.getExpressionVisitor(), arg));
		return names;
	}
	
	@Override
	public TCNameSet caseLocalDefinition(TCLocalDefinition node, EnvTriple arg)
	{
		TCNameSet names = node.type.apply(visitorSet.getTypeVisitor(), arg);
		
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
			names.addAll(nt.type.apply(visitorSet.getTypeVisitor(), arg));
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
			names.addAll(node.type.apply(visitorSet.getTypeVisitor(), arg));
		}
		
		names.addAll(node.exp.apply(visitorSet.getExpressionVisitor(), arg));
		return names;
	}
}
