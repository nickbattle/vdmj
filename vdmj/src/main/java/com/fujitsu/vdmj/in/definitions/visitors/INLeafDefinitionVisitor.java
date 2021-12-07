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

package com.fujitsu.vdmj.in.definitions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.definitions.INAssignmentDefinition;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassInvariantDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INEqualsDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INExplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INExternalDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitOperationDefinition;
import com.fujitsu.vdmj.in.definitions.INImportedDefinition;
import com.fujitsu.vdmj.in.definitions.INInheritedDefinition;
import com.fujitsu.vdmj.in.definitions.INInstanceVariableDefinition;
import com.fujitsu.vdmj.in.definitions.INLocalDefinition;
import com.fujitsu.vdmj.in.definitions.INMultiBindListDefinition;
import com.fujitsu.vdmj.in.definitions.INMutexSyncDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.definitions.INPerSyncDefinition;
import com.fujitsu.vdmj.in.definitions.INQualifiedDefinition;
import com.fujitsu.vdmj.in.definitions.INRenamedDefinition;
import com.fujitsu.vdmj.in.definitions.INStateDefinition;
import com.fujitsu.vdmj.in.definitions.INThreadDefinition;
import com.fujitsu.vdmj.in.definitions.INTypeDefinition;
import com.fujitsu.vdmj.in.definitions.INUntypedDefinition;
import com.fujitsu.vdmj.in.definitions.INValueDefinition;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.traces.INTraceApplyExpression;
import com.fujitsu.vdmj.in.traces.INTraceBracketedExpression;
import com.fujitsu.vdmj.in.traces.INTraceConcurrentExpression;
import com.fujitsu.vdmj.in.traces.INTraceCoreDefinition;
import com.fujitsu.vdmj.in.traces.INTraceDefinition;
import com.fujitsu.vdmj.in.traces.INTraceDefinitionTerm;
import com.fujitsu.vdmj.in.traces.INTraceLetBeStBinding;
import com.fujitsu.vdmj.in.traces.INTraceLetDefBinding;
import com.fujitsu.vdmj.in.traces.INTraceRepeatDefinition;
import com.fujitsu.vdmj.in.types.INPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCField;

/**
 * This INDefinition visitor visits all of the leaves of a definition tree and calls
 * the basic processing methods for the simple statements and expressions.
 */
abstract public class INLeafDefinitionVisitor<E, C extends Collection<E>, S> extends INDefinitionVisitor<C, S>
{
	protected INVisitorSet<E, C, S> visitorSet = new INVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			definitionVisitor = INLeafDefinitionVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return INLeafDefinitionVisitor.this.newCollection();
		}
	};

 	@Override
	public C caseAssignmentDefinition(INAssignmentDefinition node, S arg)
	{
		C all = newCollection();
		all.addAll(visitorSet.applyTypeVisitor(node.getType(), arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
		return all;
	}

 	@Override
	public C caseClassDefinition(INClassDefinition node, S arg)
	{
 		C all = newCollection();
 		
 		for (INDefinition def: node.definitions)
 		{
 			all.addAll(def.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseClassInvariantDefinition(INClassInvariantDefinition node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.expression, arg);
	}

 	@Override
	public C caseEqualsDefinition(INEqualsDefinition node, S arg)
	{
		C all = newCollection();
		all.addAll(visitorSet.applyTypeVisitor(node.getType(), arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.test, arg));
		return all;
	}

 	@Override
	public C caseExplicitFunctionDefinition(INExplicitFunctionDefinition node, S arg)
	{
		C all = newCollection();
		
		for (INPatternList plist: node.paramPatternList)
		{
			for (INPattern p: plist)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		all.addAll(visitorSet.applyTypeVisitor(node.getType(), arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));

		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}

		if (node.measureDef != null)
		{
			all.addAll(node.measureDef.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseExplicitOperationDefinition(INExplicitOperationDefinition node, S arg)
	{
		C all = newCollection();
		
		for (INPattern p: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}

		all.addAll(visitorSet.applyTypeVisitor(node.getType(), arg));
		all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		
		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseExternalDefinition(INExternalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseImplicitFunctionDefinition(INImplicitFunctionDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.getType(), arg);
		
		for (INPatternListTypePair ptp: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyTypeVisitor(ptp.type, arg));

			for (INPattern p: ptp.patterns)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		if (node.body != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));
		}

		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}
		
		if (node.measureDef != null)
		{
			all.addAll(node.measureDef.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseImplicitOperationDefinition(INImplicitOperationDefinition node, S arg)
	{
		C all = newCollection();
		
		for (INPatternListTypePair ptp: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyTypeVisitor(ptp.type, arg));

			for (INPattern p: ptp.patterns)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}
		
		all.addAll(visitorSet.applyTypeVisitor(node.getType(), arg));

		if (node.body != null)
		{
			all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		}

		if (node.predef != null)
		{
			all.addAll(node.predef.apply(this, arg));
		}
		
		if (node.postdef != null)
		{
			all.addAll(node.postdef.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseImportedDefinition(INImportedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInheritedDefinition(INInheritedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInstanceVariableDefinition(INInstanceVariableDefinition node, S arg)
	{
 		if (node.expression != null)
 		{
 			return caseAssignmentDefinition(node, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseLocalDefinition(INLocalDefinition node, S arg)
	{
		return visitorSet.applyTypeVisitor(node.getType(), arg);
	}

 	@Override
	public C caseMultiBindListDefinition(INMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (INMultipleBind bind: node.bindings)
 		{
 			all.addAll(visitorSet.applyMultiBindVisitor(bind, arg));
 		}
		
		return all;
	}

 	@Override
	public C caseMutexSyncDefinition(INMutexSyncDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseNamedTraceDefinition(INNamedTraceDefinition node, S arg)
	{
		C all = newCollection();
		
		for (INTraceDefinitionTerm term: node.terms)
		{
			for (INTraceDefinition tdef: term)
			{
				all.addAll(caseTraceDefinition(tdef, arg));
			}
		}
		
		return all;
	}
 	
 	private C caseTraceDefinition(INTraceDefinition tdef, S arg)
 	{
		C all = newCollection();
		
		if (tdef instanceof INTraceLetDefBinding)
		{
			INTraceLetDefBinding letdef = (INTraceLetDefBinding)tdef;
			
			for (INDefinition ldef: letdef.localDefs)
			{
				all.addAll(ldef.apply(this, arg));
			}
			
			all.addAll(caseTraceDefinition(letdef.body, arg));
		}
		else if (tdef instanceof INTraceLetBeStBinding)
		{
			INTraceLetBeStBinding letbe = (INTraceLetBeStBinding)tdef;
			all.addAll(visitorSet.applyMultiBindVisitor(letbe.bind, arg));
			all.addAll(caseTraceDefinition(letbe.body, arg));
		}
		else if (tdef instanceof INTraceRepeatDefinition)
		{
			INTraceRepeatDefinition repeat = (INTraceRepeatDefinition)tdef;
			all.addAll(caseTraceCoreDefinition(repeat.core, arg));
		}

		return all;
 	}

 	private C caseTraceCoreDefinition(INTraceCoreDefinition core, S arg)
	{
		C all = newCollection();
		
		if (core instanceof INTraceApplyExpression)
		{
			INTraceApplyExpression apply = (INTraceApplyExpression)core;
			all.addAll(visitorSet.applyStatementVisitor(apply.callStatement, arg));
		}
		else if (core instanceof INTraceBracketedExpression)
		{
			INTraceBracketedExpression bexp = (INTraceBracketedExpression)core;
			
			for (INTraceDefinitionTerm term: bexp.terms)
			{
				for (INTraceDefinition tdef: term)
				{
					all.addAll(caseTraceDefinition(tdef, arg));
				}
			}
		}
		else if (core instanceof INTraceConcurrentExpression)
		{
			INTraceConcurrentExpression cexp = (INTraceConcurrentExpression)core;
			
			for (INTraceDefinition tdef: cexp.defs)
			{
				all.addAll(caseTraceDefinition(tdef, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C casePerSyncDefinition(INPerSyncDefinition node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.guard, arg);
	}

 	@Override
	public C caseQualifiedDefinition(INQualifiedDefinition node, S arg)
	{
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseRenamedDefinition(INRenamedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseStateDefinition(INStateDefinition node, S arg)
	{
		C all = newCollection();
		
		for (TCField field: node.fields)
		{
			all.addAll(visitorSet.applyTypeVisitor(field.type, arg));
		}

		if (node.invExpression != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.invExpression, arg));
		}

		if (node.initExpression != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.initExpression, arg));
		}
		
		return all;
	}

 	@Override
	public C caseThreadDefinition(INThreadDefinition node, S arg)
	{
		return visitorSet.applyStatementVisitor(node.statement, arg);
	}

 	@Override
	public C caseTypeDefinition(INTypeDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);
		
		if (node.invdef != null)
		{
			all.addAll(node.invdef.apply(this, arg));
		}

		if (node.eqdef != null)
		{
			all.addAll(node.eqdef.apply(this, arg));
		}

		if (node.orddef != null)
		{
			all.addAll(node.orddef.apply(this, arg));
		}

		if (node.mindef != null)
		{
			all.addAll(node.mindef.apply(this, arg));
		}

		if (node.maxdef != null)
		{
			all.addAll(node.maxdef.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseUntypedDefinition(INUntypedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseValueDefinition(INValueDefinition node, S arg)
	{
		C all = newCollection();
		all.addAll(visitorSet.applyTypeVisitor(node.getType(), arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		return all;
	}
	
	abstract protected C newCollection();
}
