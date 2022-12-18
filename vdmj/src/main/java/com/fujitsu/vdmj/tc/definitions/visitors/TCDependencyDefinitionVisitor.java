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
import com.fujitsu.vdmj.tc.expressions.visitors.TCDependencyExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.visitors.TCDependencyBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCDependencyMultipleBindVisitor;
import com.fujitsu.vdmj.tc.statements.visitors.TCDependencyStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.visitors.TCDependencyTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCDependencyDefinitionVisitor extends TCLeafDefinitionVisitor<TCNameToken, TCNameSet, EnvTriple>
{
	public TCDependencyDefinitionVisitor()
	{
		visitorSet = new TCVisitorSet<TCNameToken, TCNameSet, EnvTriple>()
		{
			@Override
			protected void setVisitors()
			{
				definitionVisitor = TCDependencyDefinitionVisitor.this;
				expressionVisitor = new TCDependencyExpressionVisitor(this);
				statementVisitor = new TCDependencyStatementVisitor(this);
				typeVisitor = new TCDependencyTypeVisitor(this);
				bindVisitor = new TCDependencyBindVisitor(this);
				multiBindVisitor = new TCDependencyMultipleBindVisitor(this); 
			}

			@Override
			protected TCNameSet newCollection()
			{
				return TCDependencyDefinitionVisitor.this.newCollection();
			}
		};
	}

	public TCDependencyDefinitionVisitor(TCVisitorSet<TCNameToken, TCNameSet, EnvTriple> visitors)
	{
		this.visitorSet = visitors;
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
		return visitorSet.applyExpressionVisitor(node.test, new EnvTriple(arg.globals, local, arg.returns));
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
		TCNameSet names = visitorSet.applyExpressionVisitor(node.body, new EnvTriple(arg.globals, local, arg.returns));
		names.addAll(visitorSet.applyDefinitionVisitor(node.predef, new EnvTriple(arg.globals, local, arg.returns)));
		names.addAll(visitorSet.applyDefinitionVisitor(node.postdef, new EnvTriple(arg.globals, local, arg.returns)));
		
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
		TCNameSet names = visitorSet.applyStatementVisitor(node.body, new EnvTriple(arg.globals, local, arg.returns));
		names.addAll(visitorSet.applyDefinitionVisitor(node.predef, new EnvTriple(arg.globals, local, arg.returns)));
		names.addAll(visitorSet.applyDefinitionVisitor(node.postdef, new EnvTriple(arg.globals, local, arg.returns)));
		
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
		TCNameSet names = visitorSet.applyExpressionVisitor(node.body, new EnvTriple(arg.globals, local, arg.returns));
		names.addAll(visitorSet.applyDefinitionVisitor(node.predef, new EnvTriple(arg.globals, local, arg.returns)));
		names.addAll(visitorSet.applyDefinitionVisitor(node.postdef, new EnvTriple(arg.globals, local, arg.returns)));
		
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
		TCNameSet names = visitorSet.applyStatementVisitor(node.body, new EnvTriple(arg.globals, local, arg.returns));
		names.addAll(visitorSet.applyDefinitionVisitor(node.predef, new EnvTriple(arg.globals, local, arg.returns)));
		names.addAll(visitorSet.applyDefinitionVisitor(node.postdef, new EnvTriple(arg.globals, local, arg.returns)));
		
		return names;
	}
	
	@Override
	public TCNameSet caseInstanceVariableDefinition(TCInstanceVariableDefinition node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		names.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		names.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
		return names;
	}
	
	@Override
	public TCNameSet caseLocalDefinition(TCLocalDefinition node, EnvTriple arg)
	{
		TCNameSet names = visitorSet.applyTypeVisitor(node.type, arg);
		names.addAll(visitorSet.applyDefinitionVisitor(node.valueDefinition, arg));
		return names;
	}
	
	@Override
	public TCNameSet caseStateDefinition(TCStateDefinition node, EnvTriple arg)
	{
		Environment local = new FlatEnvironment(node, arg.env);
		TCNameSet names = new TCNameSet();
		names.addAll(visitorSet.applyDefinitionVisitor(node.invdef, new EnvTriple(arg.globals, local, arg.returns)));
		names.addAll(visitorSet.applyDefinitionVisitor(node.initdef, new EnvTriple(arg.globals, local, arg.returns)));
		return names;
	}
	
	@Override
	public TCNameSet caseTypeDefinition(TCTypeDefinition node, EnvTriple arg)
	{
		TCNameSet names = new TCNameSet();
		
		if (node.type instanceof TCNamedType)
		{
			TCNamedType nt = (TCNamedType)node.type;
			names.addAll(visitorSet.applyTypeVisitor(nt.type, arg));
		}
		else if (node.type instanceof TCRecordType)
		{
			TCRecordType rt = (TCRecordType)node.type;
			
			for (TCField field: rt.fields)
			{
				names.addAll(visitorSet.applyTypeVisitor(field.type, arg));
			}
		}
		
		names.addAll(visitorSet.applyDefinitionVisitor(node.invdef, arg));
		return names;
	}
	
	@Override
	public TCNameSet caseValueDefinition(TCValueDefinition node, EnvTriple arg)
	{
		TCNameSet names = visitorSet.applyTypeVisitor(node.type, arg);
		names.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		return names;
	}
}
