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

package com.fujitsu.vdmj.ast.definitions.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.ast.ASTVisitorSet;
import com.fujitsu.vdmj.ast.definitions.ASTAssignmentDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassInvariantDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTEqualsDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExternalDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImportedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTInheritedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTInstanceVariableDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTLocalDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTMultiBindListDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTMutexSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTNamedTraceDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTPerSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTRenamedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTThreadDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTTypeDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTUntypedDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBind;
import com.fujitsu.vdmj.ast.traces.ASTTraceApplyExpression;
import com.fujitsu.vdmj.ast.traces.ASTTraceBracketedExpression;
import com.fujitsu.vdmj.ast.traces.ASTTraceConcurrentExpression;
import com.fujitsu.vdmj.ast.traces.ASTTraceCoreDefinition;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinition;
import com.fujitsu.vdmj.ast.traces.ASTTraceDefinitionTerm;
import com.fujitsu.vdmj.ast.traces.ASTTraceLetBeStBinding;
import com.fujitsu.vdmj.ast.traces.ASTTraceLetDefBinding;
import com.fujitsu.vdmj.ast.traces.ASTTraceRepeatDefinition;
import com.fujitsu.vdmj.ast.types.ASTField;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePair;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.ast.types.ASTUnknownType;

/**
 * This ASTDefinition visitor visits all of the leaves of a definition tree and calls
 * the basic processing methods for the simple statements and expressions.
 */
abstract public class ASTLeafDefinitionVisitor<E, C extends Collection<E>, S> extends ASTDefinitionVisitor<C, S>
{
	protected ASTVisitorSet<E, C, S> visitorSet = new ASTVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			// None
		}

		@Override
		protected C newCollection()
		{
			return null;
		}
	};

 	@Override
	public C caseAssignmentDefinition(ASTAssignmentDefinition node, S arg)
	{
		C all = newCollection();
		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));	
		all.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
		return all;
	}

 	@Override
	public C caseClassDefinition(ASTClassDefinition node, S arg)
	{
 		C all = newCollection();
 		
 		for (ASTDefinition def: node.definitions)
 		{
 			all.addAll(def.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseClassInvariantDefinition(ASTClassInvariantDefinition node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.expression, arg);
	}

 	@Override
	public C caseEqualsDefinition(ASTEqualsDefinition node, S arg)
	{
		C all = newCollection();
		
		ASTType type = node.typebind != null ? node.typebind.type : new ASTUnknownType(node.location);
		all.addAll(visitorSet.applyTypeVisitor(type, arg));	
		all.addAll(visitorSet.applyExpressionVisitor(node.test, arg));
		
		return all;
	}

 	@Override
	public C caseExplicitFunctionDefinition(ASTExplicitFunctionDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));
		
		if (node.precondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		}
		
		if (node.postcondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		}
		
		if (node.measure != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.measure, arg));
		}
		
		return all;
	}

 	@Override
	public C caseExplicitOperationDefinition(ASTExplicitOperationDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);
		all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		
		if (node.precondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		}
		
		if (node.postcondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		}
		
		return all;
	}

	@Override
	public C caseExternalDefinition(ASTExternalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseImplicitFunctionDefinition(ASTImplicitFunctionDefinition node, S arg)
	{
		C all = newCollection();
		ASTTypeList ptypes = new ASTTypeList();

		for (ASTPatternListTypePair ptp: node.parameterPatterns)
		{
			for (int i=0; i<ptp.patterns.size(); i++)
			{
				ptypes.add(ptp.type);
			}
		}

		// NB: implicit functions are always +> total, apparently
		ASTFunctionType type = new ASTFunctionType(node.location, false, ptypes, node.result.type);
		all.addAll(visitorSet.applyTypeVisitor(type, arg));
		
		if (node.body != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));
		}
		
		if (node.precondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		}
		
		if (node.postcondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		}
		
		if (node.measureExp != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.measureExp, arg));
		}
		
		return all;
	}

 	@Override
	public C caseImplicitOperationDefinition(ASTImplicitOperationDefinition node, S arg)
	{
		C all = newCollection();
		ASTTypeList ptypes = new ASTTypeList();

		for (ASTPatternListTypePair ptp: node.parameterPatterns)
		{
			for (int i=0; i<ptp.patterns.size(); i++)
			{
				ptypes.add(ptp.type);
			}
		}

		// NB: implicit functions are always +> total, apparently
		ASTFunctionType type = new ASTFunctionType(node.location, false, ptypes, node.result.type);
		all.addAll(visitorSet.applyTypeVisitor(type, arg));
		
		if (node.body != null)
		{
			all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		}
		
		if (node.precondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		}
		
		if (node.postcondition != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		}

		return all;
	}

 	@Override
	public C caseImportedDefinition(ASTImportedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInheritedDefinition(ASTInheritedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInstanceVariableDefinition(ASTInstanceVariableDefinition node, S arg)
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
	public C caseLocalDefinition(ASTLocalDefinition node, S arg)
	{
		return visitorSet.applyTypeVisitor(node.type, arg);
	}

 	@Override
	public C caseMultiBindListDefinition(ASTMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (ASTMultipleBind mbind: node.bindings)
 		{
 			all.addAll(visitorSet.applyMultiBindVisitor(mbind, arg));
 		}
		
		return all;
	}

 	@Override
	public C caseMutexSyncDefinition(ASTMutexSyncDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseNamedTraceDefinition(ASTNamedTraceDefinition node, S arg)
	{
		C all = newCollection();
		
		for (ASTTraceDefinitionTerm term: node.terms)
		{
			for (ASTTraceDefinition tdef: term)
			{
				all.addAll(caseTraceDefinition(tdef, arg));
			}
		}
		
		return all;
	}
 	
 	private C caseTraceDefinition(ASTTraceDefinition tdef, S arg)
 	{
		C all = newCollection();
		
		if (tdef instanceof ASTTraceLetDefBinding)
		{
			ASTTraceLetDefBinding letdef = (ASTTraceLetDefBinding)tdef;
			
			for (ASTDefinition ldef: letdef.localDefs)
			{
				all.addAll(ldef.apply(this, arg));
			}
			
			all.addAll(caseTraceDefinition(letdef.body, arg));
		}
		else if (tdef instanceof ASTTraceLetBeStBinding)
		{
			ASTTraceLetBeStBinding letbe = (ASTTraceLetBeStBinding)tdef;
			all.addAll(visitorSet.applyMultiBindVisitor(letbe.bind, arg));
			all.addAll(caseTraceDefinition(letbe.body, arg));
		}
		else if (tdef instanceof ASTTraceRepeatDefinition)
		{
			ASTTraceRepeatDefinition repeat = (ASTTraceRepeatDefinition)tdef;
			all.addAll(caseTraceCoreDefinition(repeat.core, arg));
		}

		return all;
 	}

 	private C caseTraceCoreDefinition(ASTTraceCoreDefinition core, S arg)
	{
		C all = newCollection();
		
		if (core instanceof ASTTraceApplyExpression)
		{
			ASTTraceApplyExpression apply = (ASTTraceApplyExpression)core;
			all.addAll(visitorSet.applyStatementVisitor(apply.callStatement, arg));
		}
		else if (core instanceof ASTTraceBracketedExpression)
		{
			ASTTraceBracketedExpression bexp = (ASTTraceBracketedExpression)core;
			
			for (ASTTraceDefinitionTerm term: bexp.terms)
			{
				for (ASTTraceDefinition tdef: term)
				{
					all.addAll(caseTraceDefinition(tdef, arg));
				}
			}
		}
		else if (core instanceof ASTTraceConcurrentExpression)
		{
			ASTTraceConcurrentExpression cexp = (ASTTraceConcurrentExpression)core;
			
			for (ASTTraceDefinition tdef: cexp.defs)
			{
				all.addAll(caseTraceDefinition(tdef, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C casePerSyncDefinition(ASTPerSyncDefinition node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.guard, arg);
	}

 	@Override
	public C caseRenamedDefinition(ASTRenamedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseStateDefinition(ASTStateDefinition node, S arg)
	{
		C all = newCollection();
		
		for (ASTField field: node.fields)
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
	public C caseThreadDefinition(ASTThreadDefinition node, S arg)
	{
		return visitorSet.applyStatementVisitor(node.statement, arg);
	}

 	@Override
	public C caseTypeDefinition(ASTTypeDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);
		
		if (node.invExpression != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.invExpression, arg));
		}

		if (node.eqExpression != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.eqExpression, arg));
		}

		if (node.ordExpression != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.ordExpression, arg));
		}
		
		return all;
	}

 	@Override
	public C caseUntypedDefinition(ASTUntypedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseValueDefinition(ASTValueDefinition node, S arg)
	{
		C all = newCollection();
		
		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		
		return all;
	}
	
	abstract protected C newCollection();
}
