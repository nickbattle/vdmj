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

package com.fujitsu.vdmj.po.statements.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.annotations.POAnnotatedStatement;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.POBind;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSeqBind;
import com.fujitsu.vdmj.po.patterns.POMultipleSetBind;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.statements.POAlwaysStatement;
import com.fujitsu.vdmj.po.statements.POAssignmentStatement;
import com.fujitsu.vdmj.po.statements.POAtomicStatement;
import com.fujitsu.vdmj.po.statements.POBlockStatement;
import com.fujitsu.vdmj.po.statements.POCallObjectStatement;
import com.fujitsu.vdmj.po.statements.POCallStatement;
import com.fujitsu.vdmj.po.statements.POCaseStmtAlternative;
import com.fujitsu.vdmj.po.statements.POCasesStatement;
import com.fujitsu.vdmj.po.statements.POCyclesStatement;
import com.fujitsu.vdmj.po.statements.PODurationStatement;
import com.fujitsu.vdmj.po.statements.POElseIfStatement;
import com.fujitsu.vdmj.po.statements.POErrorStatement;
import com.fujitsu.vdmj.po.statements.POExitStatement;
import com.fujitsu.vdmj.po.statements.POForAllStatement;
import com.fujitsu.vdmj.po.statements.POForIndexStatement;
import com.fujitsu.vdmj.po.statements.POForPatternBindStatement;
import com.fujitsu.vdmj.po.statements.POIfStatement;
import com.fujitsu.vdmj.po.statements.POLetBeStStatement;
import com.fujitsu.vdmj.po.statements.POLetDefStatement;
import com.fujitsu.vdmj.po.statements.POPeriodicStatement;
import com.fujitsu.vdmj.po.statements.POReturnStatement;
import com.fujitsu.vdmj.po.statements.POSimpleBlockStatement;
import com.fujitsu.vdmj.po.statements.POSkipStatement;
import com.fujitsu.vdmj.po.statements.POSpecificationStatement;
import com.fujitsu.vdmj.po.statements.POSporadicStatement;
import com.fujitsu.vdmj.po.statements.POStartStatement;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.po.statements.POStopStatement;
import com.fujitsu.vdmj.po.statements.POSubclassResponsibilityStatement;
import com.fujitsu.vdmj.po.statements.POTixeStatement;
import com.fujitsu.vdmj.po.statements.POTixeStmtAlternative;
import com.fujitsu.vdmj.po.statements.POTrapStatement;
import com.fujitsu.vdmj.po.statements.POWhileStatement;

/**
 * This POStatement visitor visits all of the leaves of an statement tree and calls
 * the basic processing methods for the simple statements.
 */
abstract public class POLeafStatementVisitor<E, C extends Collection<E>, S> extends POStatementVisitor<C, S>
{
	protected POVisitorSet<E, C, S> visitorSet = new POVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			statementVisitor = POLeafStatementVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return POLeafStatementVisitor.this.newCollection();
		}
	};
	
	private final boolean allNodes;
	
	public POLeafStatementVisitor(boolean allNodes)
	{
		this.allNodes = allNodes;
	}
	
 	@Override
	public C caseAnnotatedStatement(POAnnotatedStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
 		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
 		
 		if (expVisitor != null)
 		{
	 		for (POExpression a: node.annotation.args)
	 		{
	 			all.addAll(a.apply(expVisitor, arg));
	 		}
 		}
 		
 		all.addAll(node.statement.apply(this, arg));
 		return all;
	}

	@Override
	public C caseAlwaysStatement(POAlwaysStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(node.always.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseAssignmentStatement(POAssignmentStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		return all;
	}

	@Override
	public C caseAtomicStatement(POAtomicStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (POAssignmentStatement assignment: node.assignments)
		{
			all.addAll(assignment.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseBlockStatement(POBlockStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		PODefinitionVisitor<C, S> defVisitor = visitorSet.getDefinitionVisitor();
		
		if (defVisitor != null)
		{
			for (PODefinition def: node.assignmentDefs)
			{
				all.addAll(def.apply(defVisitor, arg));
			}
		}
		
		for (POStatement statement: node.statements)
		{
			all.addAll(statement.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseCallObjectStatement(POCallObjectStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			for (POExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseCallStatement(POCallStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			for (POExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseCasesStatement(POCasesStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (POCaseStmtAlternative alternative: node.cases)
		{
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCyclesStatement(POCyclesStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.cycles.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseDurationStatement(PODurationStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.duration.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElseIfStatement(POElseIfStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.elseIfExp.apply(expVisitor, arg));
		}
		
		all.addAll(node.thenStmt.apply(this, arg));
		return all;
	}

 	@Override
	public C caseErrorStatement(POErrorStatement node, S arg)
	{
		return (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
	}

 	@Override
	public C caseExitStatement(POExitStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
 		if (node.expression != null)
 		{
			POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
			
			if (expVisitor != null)
			{
				all.addAll(node.expression.apply(expVisitor, arg));
			}
 		}

 		return all;
	}

 	@Override
	public C caseForAllStatement(POForAllStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.set.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForIndexStatement(POForIndexStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.from.apply(expVisitor, arg));
			all.addAll(node.to.apply(expVisitor, arg));
			
			if (node.by != null)
			{
				all.addAll(node.by.apply(expVisitor, arg));
			}
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForPatternBindStatement(POForPatternBindStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		all.addAll(caseBind(node.patternBind.bind, arg));
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIfStatement(POIfStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.ifExp.apply(expVisitor, arg));
		}
		
		all.addAll(node.thenStmt.apply(this, arg));
		
		if (node.elseList != null)
		{
			for (POElseIfStatement elseStmt: node.elseList)
			{
				all.addAll(elseStmt.apply(this, arg));
			}
		}
		
		if (node.elseStmt != null)
		{
			all.addAll(node.elseStmt.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseLetBeStStatement(POLetBeStStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		all.addAll(caseMultipleBind(node.bind, arg));
		
		if (expVisitor != null && node.suchThat != null)
		{
			all.addAll(node.suchThat.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetDefStatement(POLetDefStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		PODefinitionVisitor<C, S> defVisitor = visitorSet.getDefinitionVisitor();
		
		if (defVisitor != null)
		{
			for (PODefinition def: node.localDefs)
			{
				all.addAll(def.apply(defVisitor, arg));
			}
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C casePeriodicStatement(POPeriodicStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			for (POExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseReturnStatement(POReturnStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
 		if (node.expression != null)
 		{
			POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
			
			if (expVisitor != null)
			{
				all.addAll(node.expression.apply(expVisitor, arg));
			}
 		}

 		return all;
	}

 	@Override
	public C caseSimpleBlockStatement(POSimpleBlockStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (POStatement stmt: node.statements)
		{
			all.addAll(stmt.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSkipStatement(POSkipStatement node, S arg)
	{
		return (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
	}

 	@Override
	public C caseSpecificationStatement(POSpecificationStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			if (node.precondition != null)
			{
				all.addAll(node.precondition.apply(expVisitor, arg));
			}
			
			if (node.postcondition != null)
			{
				all.addAll(node.postcondition.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseSporadicStatement(POSporadicStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			for (POExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseStartStatement(POStartStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.objects.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseStopStatement(POStopStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.objects.apply(expVisitor, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSubclassResponsibilityStatement(POSubclassResponsibilityStatement node, S arg)
	{
		return (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
	}

 	@Override
	public C caseTixeStatement(POTixeStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (POTixeStmtAlternative alternative: node.traps)
		{
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTrapStatement(POTrapStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(caseBind(node.patternBind.bind, arg));
		all.addAll(node.with.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseWhileStatement(POWhileStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

	private C caseBind(POBind bind, S arg)
	{
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof POSetBind)
			{
				POSetBind sbind = (POSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof POSeqBind)
			{
				POSeqBind sbind = (POSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	private C caseMultipleBind(POMultipleBind bind, S arg)
	{
		POExpressionVisitor<C, S> expVisitor = visitorSet.getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof POMultipleSetBind)
			{
				POMultipleSetBind sbind = (POMultipleSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof POMultipleSeqBind)
			{
				POMultipleSeqBind sbind = (POMultipleSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}
	
	abstract protected C newCollection();
	
	protected C caseNonLeafNode(POStatement stmt, S arg)
	{
		throw new RuntimeException("caseNonLeafNode must be overridden if allNodes is set");
	}
}
