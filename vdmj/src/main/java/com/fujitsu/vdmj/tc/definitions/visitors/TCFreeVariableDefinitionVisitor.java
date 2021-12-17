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
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCFreeVariableExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.visitors.TCFreeVariableBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCFreeVariableMultipleBindVisitor;
import com.fujitsu.vdmj.tc.patterns.visitors.TCFreeVariablePatternVisitor;
import com.fujitsu.vdmj.tc.statements.visitors.TCFreeVariableStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.visitors.TCFreeVariableTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCFreeVariableDefinitionVisitor extends TCLeafDefinitionVisitor<TCNameToken, TCNameSet, Environment>
{
	public TCFreeVariableDefinitionVisitor()
	{
		visitorSet = new TCVisitorSet<TCNameToken, TCNameSet, Environment>()
		{
			@Override
			protected void setVisitors()
			{
				definitionVisitor = TCFreeVariableDefinitionVisitor.this;
				expressionVisitor = new TCFreeVariableExpressionVisitor(this);
				statementVisitor = new TCFreeVariableStatementVisitor(this);
				patternVisitor = new TCFreeVariablePatternVisitor(this);
				typeVisitor = new TCFreeVariableTypeVisitor(this);
				bindVisitor = new TCFreeVariableBindVisitor(this);
				multiBindVisitor = new TCFreeVariableMultipleBindVisitor(this); 
			}

			@Override
			protected TCNameSet newCollection()
			{
				return TCFreeVariableDefinitionVisitor.this.newCollection();
			}
		};
	}

	public TCFreeVariableDefinitionVisitor(TCVisitorSet<TCNameToken, TCNameSet, Environment> visitors)
	{
		assert visitors != null : "Visitor set cannot be null";
		this.visitorSet = visitors;
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseDefinition(TCDefinition node, Environment arg)
	{
		return newCollection();
	}

	@Override
	public TCNameSet caseEqualsDefinition(TCEqualsDefinition node, Environment arg)
	{
		Environment local = new FlatEnvironment(node.defs, arg);
		return visitorSet.applyExpressionVisitor(node.test, local);
	}
	
	@Override
	public TCNameSet caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, Environment arg)
	{
		TCDefinitionList defs = new TCDefinitionList();
		
		if (node.paramDefinitionList != null)
		{
    		for (TCDefinitionList pdef: node.paramDefinitionList)
    		{
    			defs.addAll(pdef);	// All definitions of all parameter lists
    		}
		}

		Environment local = new FlatEnvironment(defs, arg);
		TCNameSet names = visitorSet.applyExpressionVisitor(node.body, local);
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, local));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, local));
		}
		
		if (node.measureDef != null)
		{
			names.addAll(node.measureDef.apply(this, arg));
		}

		return names;
	}
	
	@Override
	public TCNameSet caseExplicitOperationDefinition(TCExplicitOperationDefinition node, Environment arg)
	{
		TCDefinitionList defs = new TCDefinitionList();
		
		if (node.paramDefinitions != null)
		{
			defs.addAll(node.paramDefinitions);
		}

		Environment local = new FlatEnvironment(defs, arg);
		TCNameSet names = visitorSet.applyStatementVisitor(node.body, local);
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, local));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, local));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, Environment arg)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCPatternListTypePair pltp: node.parameterPatterns)
		{
			defs.addAll(pltp.getDefinitions(NameScope.LOCAL));
		}

		Environment local = new FlatEnvironment(defs, arg);
		TCNameSet names = new TCNameSet();
		
		if (node.body != null)
		{
			names.addAll(visitorSet.applyExpressionVisitor(node.body, local));
		}
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, local));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, local));
		}
		
		if (node.measureDef != null)
		{
			names.addAll(node.measureDef.apply(this, arg));
		}

		return names;
	}
	
	@Override
	public TCNameSet caseImplicitOperationDefinition(TCImplicitOperationDefinition node, Environment arg)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCPatternListTypePair pltp: node.parameterPatterns)
		{
			defs.addAll(pltp.getDefinitions(NameScope.LOCAL));
		}

		Environment local = new FlatEnvironment(defs, arg);
		TCNameSet names = new TCNameSet();
		
		if (node.body != null)
		{
			names.addAll(visitorSet.applyStatementVisitor(node.body, local));
		}
		
		if (node.predef != null)
		{
			names.addAll(node.predef.apply(this, local));
		}
		
		if (node.postdef != null)
		{
			names.addAll(node.postdef.apply(this, local));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseLocalDefinition(TCLocalDefinition node, Environment arg)
	{
		TCNameSet names = visitorSet.applyTypeVisitor(node.type, arg);
		
		if (node.valueDefinition != null)
		{
			names.addAll(node.valueDefinition.apply(this, arg));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseStateDefinition(TCStateDefinition node, Environment arg)
	{
		Environment local = new FlatEnvironment(node, arg);
		TCNameSet names = new TCNameSet();
		
		if (node.invdef != null)
		{
			names.addAll(node.invdef.apply(this, local));
		}
		
		if (node.initdef != null)
		{
			names.addAll(node.initdef.apply(this, local));
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseValueDefinition(TCValueDefinition node, Environment arg)
	{
		TCNameSet names = new TCNameSet();
		
		if (node.type != null)
		{
			names.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		}
		
		names.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		return names;
	}
}
