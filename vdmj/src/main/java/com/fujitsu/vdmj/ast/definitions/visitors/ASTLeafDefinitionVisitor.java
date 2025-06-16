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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
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
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.statements.ASTErrorCase;
import com.fujitsu.vdmj.ast.statements.ASTExternalClause;
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
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePair;

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
			definitionVisitor = ASTLeafDefinitionVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return ASTLeafDefinitionVisitor.this.newCollection();
		}
	};

 	@Override
	public C caseAssignmentDefinition(ASTAssignmentDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);	
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
		C all = visitorSet.applyPatternVisitor(node.pattern, arg);
		all.addAll(visitorSet.applyBindVisitor(node.typebind, arg));	
		all.addAll(visitorSet.applyBindVisitor(node.bind, arg));	
		all.addAll(visitorSet.applyExpressionVisitor(node.test, arg));
		return all;
	}

 	@Override
	public C caseExplicitFunctionDefinition(ASTExplicitFunctionDefinition node, S arg)
	{
		C all = newCollection();

		for (ASTPatternList plist: node.paramPatternList)
		{
			for (ASTPattern p: plist)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.measure, arg));
		
		return all;
	}

 	@Override
	public C caseExplicitOperationDefinition(ASTExplicitOperationDefinition node, S arg)
	{
		C all = newCollection();

		for (ASTPattern p: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyPatternVisitor(p, arg));
		}

		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		
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

		for (ASTPatternListTypePair ptp: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyTypeVisitor(ptp.type, arg));

			for (ASTPattern p: ptp.patterns)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		all.addAll(visitorSet.applyPatternVisitor(node.result.pattern, arg));
		all.addAll(visitorSet.applyTypeVisitor(node.result.type, arg));
		
		all.addAll(visitorSet.applyExpressionVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.measureExp, arg));
		
		return all;
	}

 	@Override
	public C caseImplicitOperationDefinition(ASTImplicitOperationDefinition node, S arg)
	{
		C all = newCollection();

		for (ASTPatternListTypePair ptp: node.parameterPatterns)
		{
			all.addAll(visitorSet.applyTypeVisitor(ptp.type, arg));

			for (ASTPattern p: ptp.patterns)
			{
				all.addAll(visitorSet.applyPatternVisitor(p, arg));
			}
		}

		if (node.result != null)
		{
			all.addAll(visitorSet.applyPatternVisitor(node.result.pattern, arg));
			all.addAll(visitorSet.applyTypeVisitor(node.result.type, arg));
		}
		
		if (node.externals != null)
		{
			for (ASTExternalClause ex: node.externals)
			{
				all.addAll(visitorSet.applyTypeVisitor(ex.type, arg));
			}
		}
		
		if (node.errors != null)
		{
			for (ASTErrorCase err: node.errors)
			{
				all.addAll(visitorSet.applyExpressionVisitor(err.left, arg));
				all.addAll(visitorSet.applyExpressionVisitor(err.right, arg));
			}
		}

		all.addAll(visitorSet.applyStatementVisitor(node.body, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));

		return all;
	}

 	@Override
	public C caseImportedDefinition(ASTImportedDefinition node, S arg)
	{
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseInheritedDefinition(ASTInheritedDefinition node, S arg)
	{
		return node.superdef.apply(this, arg);
	}

 	@Override
	public C caseInstanceVariableDefinition(ASTInstanceVariableDefinition node, S arg)
	{
		C all = visitorSet.applyTypeVisitor(node.type, arg);	
		all.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
		return all;
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
			all.addAll(visitorSet.applyExpressionVisitor(letbe.stexp, arg));
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
		return node.def.apply(this, arg);
	}

 	@Override
	public C caseStateDefinition(ASTStateDefinition node, S arg)
	{
		C all = newCollection();
		
		for (ASTField field: node.fields)
		{
			all.addAll(visitorSet.applyTypeVisitor(field.type, arg));
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.invExpression, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.initExpression, arg));
		
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
		all.addAll(visitorSet.applyPatternVisitor(node.invPattern, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.invExpression, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.eqPattern1, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.eqPattern2, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.eqExpression, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.ordPattern1, arg));
		all.addAll(visitorSet.applyPatternVisitor(node.ordPattern2, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.ordExpression, arg));
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
		C all = visitorSet.applyPatternVisitor(node.pattern, arg);
		all.addAll(visitorSet.applyTypeVisitor(node.type, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		return all;
	}
	
	abstract protected C newCollection();
}
