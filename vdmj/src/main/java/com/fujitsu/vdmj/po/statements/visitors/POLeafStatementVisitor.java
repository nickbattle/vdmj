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
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.POAlwaysStatement;
import com.fujitsu.vdmj.po.statements.POAssignmentStatement;
import com.fujitsu.vdmj.po.statements.POAtomicStatement;
import com.fujitsu.vdmj.po.statements.POBlockStatement;
import com.fujitsu.vdmj.po.statements.POCallObjectStatement;
import com.fujitsu.vdmj.po.statements.POCallStatement;
import com.fujitsu.vdmj.po.statements.POCaseStmtAlternative;
import com.fujitsu.vdmj.po.statements.POCasesStatement;
import com.fujitsu.vdmj.po.statements.POCyclesStatement;
import com.fujitsu.vdmj.po.statements.PODefStatement;
import com.fujitsu.vdmj.po.statements.PODurationStatement;
import com.fujitsu.vdmj.po.statements.POElseIfStatement;
import com.fujitsu.vdmj.po.statements.POErrorCase;
import com.fujitsu.vdmj.po.statements.POErrorStatement;
import com.fujitsu.vdmj.po.statements.POExitStatement;
import com.fujitsu.vdmj.po.statements.POExternalClause;
import com.fujitsu.vdmj.po.statements.POFieldDesignator;
import com.fujitsu.vdmj.po.statements.POForAllStatement;
import com.fujitsu.vdmj.po.statements.POForIndexStatement;
import com.fujitsu.vdmj.po.statements.POForPatternBindStatement;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POIfStatement;
import com.fujitsu.vdmj.po.statements.POLetBeStStatement;
import com.fujitsu.vdmj.po.statements.POLetDefStatement;
import com.fujitsu.vdmj.po.statements.POMapSeqDesignator;
import com.fujitsu.vdmj.po.statements.POObjectApplyDesignator;
import com.fujitsu.vdmj.po.statements.POObjectDesignator;
import com.fujitsu.vdmj.po.statements.POObjectFieldDesignator;
import com.fujitsu.vdmj.po.statements.POObjectIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POObjectNewDesignator;
import com.fujitsu.vdmj.po.statements.POObjectSelfDesignator;
import com.fujitsu.vdmj.po.statements.POPeriodicStatement;
import com.fujitsu.vdmj.po.statements.POReturnStatement;
import com.fujitsu.vdmj.po.statements.POSimpleBlockStatement;
import com.fujitsu.vdmj.po.statements.POSkipStatement;
import com.fujitsu.vdmj.po.statements.POSpecificationStatement;
import com.fujitsu.vdmj.po.statements.POSporadicStatement;
import com.fujitsu.vdmj.po.statements.POStartStatement;
import com.fujitsu.vdmj.po.statements.POStateDesignator;
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
	
	public POVisitorSet<E, C, S> getVistorSet()
	{
		return visitorSet;
	}
	
	private final boolean allNodes;
	
	public POLeafStatementVisitor(boolean allNodes)
	{
		this.allNodes = allNodes;
	}
	
 	@Override
	public C caseAnnotatedStatement(POAnnotatedStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
 		
 		for (POExpression a: node.annotation.args)
 		{
 			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
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
		all.addAll(caseStateDesignator(node.target, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
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
		
		for (PODefinition def: node.assignmentDefs)
		{
			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
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
		all.addAll(caseObjectDesignator(node.designator, arg));
		
		for (POExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCallStatement(POCallStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (POExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
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
		all.addAll(visitorSet.applyExpressionVisitor(node.cycles, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseDurationStatement(PODurationStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.duration, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}
 	
 	@Override
 	public C caseDefStatement(PODefStatement node, S arg)
 	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (PODefinition def: node.equalsDefs)
		{
			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
 	}

 	@Override
	public C caseElseIfStatement(POElseIfStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.elseIfExp, arg));
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
			all.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
 		}

 		return all;
	}

 	@Override
	public C caseForAllStatement(POForAllStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyPatternVisitor(node.pattern, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.set, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForIndexStatement(POForIndexStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.from, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.to, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.by, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForPatternBindStatement(POForPatternBindStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyPatternVisitor(node.patternBind.pattern, arg));
		all.addAll(visitorSet.applyBindVisitor(node.patternBind.bind, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIfStatement(POIfStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		all.addAll(visitorSet.applyExpressionVisitor(node.ifExp, arg));
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
		all.addAll(visitorSet.applyMultiBindVisitor(node.bind, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.suchThat, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetDefStatement(POLetDefStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (PODefinition def: node.localDefs)
		{
 			if (def instanceof POValueDefinition)
 			{
 				POValueDefinition vdef = (POValueDefinition)def;
 				all.addAll(visitorSet.applyExpressionVisitor(vdef.exp, arg));
 			}
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C casePeriodicStatement(POPeriodicStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (POExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseReturnStatement(POReturnStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
 		if (node.expression != null)
 		{
			all.addAll(visitorSet.applyExpressionVisitor(node.expression, arg));
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
		
		all.addAll(visitorSet.applyExpressionVisitor(node.precondition, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));

		if (node.externals != null)
		{
			for (POExternalClause ex: node.externals)
			{
				all.addAll(visitorSet.applyTypeVisitor(ex.type, arg));
			}
		}
		
		if (node.errors != null)
		{
			for (POErrorCase err: node.errors)
			{
				all.addAll(visitorSet.applyExpressionVisitor(err.left, arg));
				all.addAll(visitorSet.applyExpressionVisitor(err.right, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseSporadicStatement(POSporadicStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		
		for (POExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseStartStatement(POStartStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.objects, arg));
		return all;
	}

 	@Override
	public C caseStopStatement(POStopStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.objects, arg));
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
			all.addAll(visitorSet.applyPatternVisitor(alternative.patternBind.pattern, arg));
			all.addAll(visitorSet.applyBindVisitor(alternative.patternBind.bind, arg));
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTrapStatement(POTrapStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyPatternVisitor(node.patternBind.pattern, arg));
		all.addAll(visitorSet.applyBindVisitor(node.patternBind.bind, arg));
		all.addAll(node.with.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseWhileStatement(POWhileStatement node, S arg)
	{
		C all = (allNodes) ? caseNonLeafNode(node, arg) : newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}
	
	private C caseStateDesignator(POStateDesignator designator, S arg)
	{
		if (designator instanceof POFieldDesignator)
		{
			POFieldDesignator fd = (POFieldDesignator)designator;
			return caseStateDesignator(fd.object, arg);
		}
		else if (designator instanceof POIdentifierDesignator)
		{
			return newCollection();
		}
		else if (designator instanceof POMapSeqDesignator)
		{
			POMapSeqDesignator msd = (POMapSeqDesignator)designator;
			C all = caseStateDesignator(msd.mapseq, arg);
			all.addAll(visitorSet.applyExpressionVisitor(msd.exp, arg));
			return all;
		}
		else
		{
			throw new IllegalArgumentException("caseStateDesignator");
		}
	}

	private C caseObjectDesignator(POObjectDesignator designator, S arg)
	{
		if (designator instanceof POObjectApplyDesignator)
		{
			POObjectApplyDesignator ad = (POObjectApplyDesignator)designator;
			C all = caseObjectDesignator(ad.object, arg);
			
			for (POExpression exp: ad.args)
			{
				all.addAll(visitorSet.applyExpressionVisitor(exp, arg));
			}
			
			return all;
		}
		else if (designator instanceof POObjectFieldDesignator)
		{
			POObjectFieldDesignator fd = (POObjectFieldDesignator)designator;
			return caseObjectDesignator(fd.object, arg);
		}
		else if (designator instanceof POObjectNewDesignator)
		{
			POObjectNewDesignator nd = (POObjectNewDesignator)designator;
			return visitorSet.applyExpressionVisitor(nd.expression, arg);
		}
		else if (designator instanceof POObjectIdentifierDesignator)
		{
			return newCollection();
		}
		else if (designator instanceof POObjectSelfDesignator)
		{
			return newCollection();
		}
		else
		{
			throw new IllegalArgumentException("caseObjectDesignator");
		}
	}

	abstract protected C newCollection();
	
	protected C caseNonLeafNode(POStatement stmt, S arg)
	{
		throw new RuntimeException("caseNonLeafNode must be overridden if allNodes is set");
	}
}
