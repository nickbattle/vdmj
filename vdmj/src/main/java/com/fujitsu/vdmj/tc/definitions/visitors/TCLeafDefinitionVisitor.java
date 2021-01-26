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

package com.fujitsu.vdmj.tc.definitions.visitors;

import java.util.Collection;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassInvariantDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCEqualsDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExternalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImportedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInheritedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMultiBindListDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCNamedTraceDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCQualifiedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCRenamedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCThreadDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCUntypedDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.patterns.visitors.TCPatternVisitor;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.traces.TCTraceApplyExpression;
import com.fujitsu.vdmj.tc.traces.TCTraceBracketedExpression;
import com.fujitsu.vdmj.tc.traces.TCTraceConcurrentExpression;
import com.fujitsu.vdmj.tc.traces.TCTraceCoreDefinition;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinition;
import com.fujitsu.vdmj.tc.traces.TCTraceDefinitionTerm;
import com.fujitsu.vdmj.tc.traces.TCTraceLetBeStBinding;
import com.fujitsu.vdmj.tc.traces.TCTraceLetDefBinding;
import com.fujitsu.vdmj.tc.traces.TCTraceRepeatDefinition;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

/**
 * This TCDefinition visitor visits all of the leaves of a definition tree and calls
 * the basic processing methods for the simple statements and expressions.
 */
abstract public class TCLeafDefinitionVisitor<E, C extends Collection<E>, S> extends TCDefinitionVisitor<C, S>
{
	protected TCVisitorSet<E, C, S> visitorSet;
	
 	@Override
	public C caseAssignmentDefinition(TCAssignmentDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.expression.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseClassDefinition(TCClassDefinition node, S arg)
	{
 		C all = newCollection();
 		
 		for (TCDefinition def: node.definitions)
 		{
 			all.addAll(def.apply(this, arg));
 		}
 		
 		return all;
	}

 	@Override
	public C caseClassInvariantDefinition(TCClassInvariantDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseEqualsDefinition(TCEqualsDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.test.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		TCPatternVisitor<C, S> patternVisitor = visitorSet.getPatternVisitor();
		C all = newCollection();
		
		if (patternVisitor != null)
		{
			all.addAll(patternCheck(patternVisitor, node.paramPatternList, arg));
		}
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.body.apply(expVisitor, arg));
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
	public C caseExplicitOperationDefinition(TCExplicitOperationDefinition node, S arg)
	{
		TCStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		TCPatternVisitor<C, S> patternVisitor = visitorSet.getPatternVisitor();
		C all = newCollection();
		
		if (patternVisitor != null)
		{
			patternCheck(patternVisitor, node.parameterPatterns, arg);
		}
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (stmtVisitor != null)
		{
			all.addAll(node.body.apply(stmtVisitor, arg));
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
	public C caseExternalDefinition(TCExternalDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		TCPatternVisitor<C, S> patternVisitor = visitorSet.getPatternVisitor();
		C all = newCollection();
		
		if (patternVisitor != null)
		{
			for (TCPatternListTypePair pair: node.parameterPatterns)
			{
				all.addAll(patternCheck(patternVisitor, pair.patterns, arg));
			}
		}

		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null && node.body != null)
		{
			all.addAll(node.body.apply(expVisitor, arg));
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
	public C caseImplicitOperationDefinition(TCImplicitOperationDefinition node, S arg)
	{
		TCStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		TCPatternVisitor<C, S> patternVisitor = visitorSet.getPatternVisitor();
		C all = newCollection();
		
		if (patternVisitor != null)
		{
			for (TCPatternListTypePair pair: node.parameterPatterns)
			{
				all.addAll(patternCheck(patternVisitor, pair.patterns, arg));
			}
		}
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (stmtVisitor != null && node.body != null)
		{
			all.addAll(node.body.apply(stmtVisitor, arg));
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
	public C caseImportedDefinition(TCImportedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInheritedDefinition(TCInheritedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseInstanceVariableDefinition(TCInstanceVariableDefinition node, S arg)
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
	public C caseLocalDefinition(TCLocalDefinition node, S arg)
	{
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseMultiBindListDefinition(TCMultiBindListDefinition node, S arg)
	{
 		C all = newCollection();
 		
		for (TCMultipleBind bind: node.bindings)
 		{
 			all.addAll(caseMultipleBind(bind, arg));
 		}
		
		return all;
	}

 	@Override
	public C caseMutexSyncDefinition(TCMutexSyncDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseNamedTraceDefinition(TCNamedTraceDefinition node, S arg)
	{
		C all = newCollection();
		
		for (TCTraceDefinitionTerm term: node.terms)
		{
			for (TCTraceDefinition tdef: term)
			{
				all.addAll(caseTraceDefinition(tdef, arg));
			}
		}
		
		return all;
	}
 	
 	private C caseTraceDefinition(TCTraceDefinition tdef, S arg)
 	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		C all = newCollection();
		
		if (tdef instanceof TCTraceLetDefBinding)
		{
			TCTraceLetDefBinding letdef = (TCTraceLetDefBinding)tdef;
			
			for (TCDefinition ldef: letdef.localDefs)
			{
				all.addAll(ldef.apply(this, arg));
			}
			
			all.addAll(caseTraceDefinition(letdef.body, arg));
		}
		else if (tdef instanceof TCTraceLetBeStBinding)
		{
			TCTraceLetBeStBinding letbe = (TCTraceLetBeStBinding)tdef;

			if (letbe.stexp != null && expVisitor != null)
			{
				all.addAll(letbe.stexp.apply(expVisitor, arg));
			}
			
			all.addAll(caseMultipleBind(letbe.bind, arg));
			all.addAll(caseTraceDefinition(letbe.body, arg));
		}
		else if (tdef instanceof TCTraceRepeatDefinition)
		{
			TCTraceRepeatDefinition repeat = (TCTraceRepeatDefinition)tdef;
			all.addAll(caseTraceCoreDefinition(repeat.core, arg));
		}

		return all;
 	}

 	private C caseTraceCoreDefinition(TCTraceCoreDefinition core, S arg)
	{
		C all = newCollection();
		
		if (core instanceof TCTraceApplyExpression)
		{
			TCStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
			
			if (stmtVisitor != null)
			{
				TCTraceApplyExpression apply = (TCTraceApplyExpression)core;
				all.addAll(apply.callStatement.apply(stmtVisitor, arg));
			}
		}
		else if (core instanceof TCTraceBracketedExpression)
		{
			TCTraceBracketedExpression bexp = (TCTraceBracketedExpression)core;
			
			for (TCTraceDefinitionTerm term: bexp.terms)
			{
				for (TCTraceDefinition tdef: term)
				{
					all.addAll(caseTraceDefinition(tdef, arg));
				}
			}
		}
		else if (core instanceof TCTraceConcurrentExpression)
		{
			TCTraceConcurrentExpression cexp = (TCTraceConcurrentExpression)core;
			
			for (TCTraceDefinition tdef: cexp.defs)
			{
				all.addAll(caseTraceDefinition(tdef, arg));
			}
		}
		
		return all;
	}

	@Override
	public C casePerSyncDefinition(TCPerSyncDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		return (expVisitor != null ? node.guard.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseQualifiedDefinition(TCQualifiedDefinition node, S arg)
	{
		return node.deref().apply(this, arg);
	}

 	@Override
	public C caseRenamedDefinition(TCRenamedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseStateDefinition(TCStateDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			for (TCField field: node.fields)
			{
				all.addAll(field.type.apply(typeVisitor, arg));
			}
		}
		
		if (expVisitor != null)
		{
			if (node.invExpression != null)
			{
				all.addAll(node.invExpression.apply(expVisitor, arg));
			}

			if (node.initExpression != null)
			{
				all.addAll(node.initExpression.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseThreadDefinition(TCThreadDefinition node, S arg)
	{
		TCStatementVisitor<C, S> stmtVisitor = visitorSet.getStatementVisitor();
		return (stmtVisitor != null ? node.statement.apply(stmtVisitor, arg) : newCollection());
	}

 	@Override
	public C caseTypeDefinition(TCTypeDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		C all = newCollection();
		
		if (typeVisitor != null)
		{
			all.addAll(node.type.apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			if (node.invExpression != null)
			{
				all.addAll(node.invExpression.apply(expVisitor, arg));
			}

			if (node.eqExpression != null)
			{
				all.addAll(node.eqExpression.apply(expVisitor, arg));
			}

			if (node.ordExpression != null)
			{
				all.addAll(node.ordExpression.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseUntypedDefinition(TCUntypedDefinition node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseValueDefinition(TCValueDefinition node, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		TCTypeVisitor<C, S> typeVisitor = visitorSet.getTypeVisitor();
		TCPatternVisitor<C, S> patternVisitor = visitorSet.getPatternVisitor();
		C all = newCollection();
		
		if (patternVisitor != null)
		{
			all.addAll(node.pattern.apply(patternVisitor, arg));
		}
		
		if (typeVisitor != null)
		{
			all.addAll(node.getType().apply(typeVisitor, arg));
		}
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		return all;
	}

 	/**
 	 * This multiple bind case covers the common expression visitor
 	 * cases, but they can be overridden, perhaps to use the (m)bind visitorSet
 	 * entry if required. 
 	 */
 	protected C caseMultipleBind(TCMultipleBind bind, S arg)
	{
		TCExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof TCMultipleSetBind)
			{
				TCMultipleSetBind sbind = (TCMultipleSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof TCMultipleSeqBind)
			{
				TCMultipleSeqBind sbind = (TCMultipleSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

	protected C patternCheck(TCPatternVisitor<C, S> patternVisitor, TCPatternListList paramPatternList, S arg)
	{
		C all = newCollection();

		for (TCPatternList list: paramPatternList)
		{
			all.addAll(patternCheck(patternVisitor, list, arg));
		}
		
		return all;
	}

	protected C patternCheck(TCPatternVisitor<C, S> patternVisitor, TCPatternList list, S arg)
	{
		C all = newCollection();

		for (TCPattern p: list)
		{
			all.addAll(p.apply(patternVisitor, arg));
		}
		
		return all;
	}
	
	abstract protected C newCollection();
}
