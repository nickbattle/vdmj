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

package com.fujitsu.vdmj.tc.statements.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.annotations.TCAnnotatedStatement;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.statements.TCAlwaysStatement;
import com.fujitsu.vdmj.tc.statements.TCAssignmentStatement;
import com.fujitsu.vdmj.tc.statements.TCAtomicStatement;
import com.fujitsu.vdmj.tc.statements.TCBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCCaseStmtAlternative;
import com.fujitsu.vdmj.tc.statements.TCCasesStatement;
import com.fujitsu.vdmj.tc.statements.TCCyclesStatement;
import com.fujitsu.vdmj.tc.statements.TCDefStatement;
import com.fujitsu.vdmj.tc.statements.TCDurationStatement;
import com.fujitsu.vdmj.tc.statements.TCElseIfStatement;
import com.fujitsu.vdmj.tc.statements.TCErrorCase;
import com.fujitsu.vdmj.tc.statements.TCErrorStatement;
import com.fujitsu.vdmj.tc.statements.TCExitStatement;
import com.fujitsu.vdmj.tc.statements.TCExternalClause;
import com.fujitsu.vdmj.tc.statements.TCFieldDesignator;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForIndexStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCIdentifierDesignator;
import com.fujitsu.vdmj.tc.statements.TCIfStatement;
import com.fujitsu.vdmj.tc.statements.TCLetBeStStatement;
import com.fujitsu.vdmj.tc.statements.TCLetDefStatement;
import com.fujitsu.vdmj.tc.statements.TCMapSeqDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectApplyDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectFieldDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectIdentifierDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectNewDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectSelfDesignator;
import com.fujitsu.vdmj.tc.statements.TCPeriodicStatement;
import com.fujitsu.vdmj.tc.statements.TCReturnStatement;
import com.fujitsu.vdmj.tc.statements.TCSimpleBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCSkipStatement;
import com.fujitsu.vdmj.tc.statements.TCSpecificationStatement;
import com.fujitsu.vdmj.tc.statements.TCSporadicStatement;
import com.fujitsu.vdmj.tc.statements.TCStartStatement;
import com.fujitsu.vdmj.tc.statements.TCStateDesignator;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.TCStopStatement;
import com.fujitsu.vdmj.tc.statements.TCSubclassResponsibilityStatement;
import com.fujitsu.vdmj.tc.statements.TCTixeStatement;
import com.fujitsu.vdmj.tc.statements.TCTixeStmtAlternative;
import com.fujitsu.vdmj.tc.statements.TCTrapStatement;
import com.fujitsu.vdmj.tc.statements.TCWhileStatement;
import com.fujitsu.vdmj.tc.traces.TCTraceVariableStatement;

/**
 * This TCStatement visitor visits all of the leaves of an statement tree and calls
 * the basic processing methods for the simple statements.
 */
abstract public class TCLeafStatementVisitor<E, C extends Collection<E>, S> extends TCStatementVisitor<C, S>
{
	protected TCVisitorSet<E, C, S> visitorSet = new TCVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			statementVisitor = TCLeafStatementVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return TCLeafStatementVisitor.this.newCollection();
		}
	};
	
	public TCVisitorSet<E, C, S> getVistorSet()
	{
		return visitorSet;
	}
	
	@Override
	public C caseAnnotatedStatement(TCAnnotatedStatement node, S arg)
	{
 		C all = newCollection();
 		
 		for (TCExpression a: node.annotation.args)
 		{
 			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
 		}
 		
 		all.addAll(node.statement.apply(this, arg));
 		return all;
	}

	@Override
	public C caseAlwaysStatement(TCAlwaysStatement node, S arg)
	{
		C all = node.always.apply(this, arg);
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseAssignmentStatement(TCAssignmentStatement node, S arg)
	{
 		C all = caseStateDesignator(node.target, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		return all;
	}

	@Override
	public C caseAtomicStatement(TCAtomicStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCAssignmentStatement assignment: node.assignments)
		{
			all.addAll(assignment.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseBlockStatement(TCBlockStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCDefinition def: node.assignmentDefs)
		{
			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
		}
		
		for (TCStatement statement: node.statements)
		{
			all.addAll(statement.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseCallObjectStatement(TCCallObjectStatement node, S arg)
	{
		C all = caseObjectDesignator(node.designator, arg);
		
		for (TCExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCallStatement(TCCallStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCasesStatement(TCCasesStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.exp, arg);
		
		for (TCCaseStmtAlternative alternative: node.cases)
		{
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCyclesStatement(TCCyclesStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.cycles, arg);
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseDurationStatement(TCDurationStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.duration, arg);
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElseIfStatement(TCElseIfStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.elseIfExp, arg);
		all.addAll(node.thenStmt.apply(this, arg));
		return all;
	}

 	@Override
	public C caseErrorStatement(TCErrorStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseExitStatement(TCExitStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.expression, arg);
	}

 	@Override
	public C caseForAllStatement(TCForAllStatement node, S arg)
	{
		C all = visitorSet.applyPatternVisitor(node.pattern, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.set, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForIndexStatement(TCForIndexStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.from, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.to, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.by, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForPatternBindStatement(TCForPatternBindStatement node, S arg)
	{
 		C all = visitorSet.applyBindVisitor(node.patternBind.bind, arg);
 		all.addAll(visitorSet.applyPatternVisitor(node.patternBind.pattern, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.seqexp, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIfStatement(TCIfStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.ifExp, arg);
		all.addAll(node.thenStmt.apply(this, arg));
		
		if (node.elseList != null)
		{
			for (TCElseIfStatement elseStmt: node.elseList)
			{
				all.addAll(elseStmt.apply(this, arg));
			}
		}
		
		if (node.elseStmt != null)
		{
			all.addAll(visitorSet.applyStatementVisitor(node.elseStmt, arg));
		}
		
		return all;
	}

 	@Override
	public C caseLetBeStStatement(TCLetBeStStatement node, S arg)
	{
		C all = visitorSet.applyMultiBindVisitor(node.bind, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.suchThat, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetDefStatement(TCLetDefStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCDefinition def: node.localDefs)
		{
			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}
 	
 	@Override
 	public C caseDefStatement(TCDefStatement node, S arg)
 	{
		C all = newCollection();
		
		for (TCDefinition def: node.equalsDefs)
		{
			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
 	}

 	@Override
	public C casePeriodicStatement(TCPeriodicStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseReturnStatement(TCReturnStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.expression, arg);
	}

 	@Override
	public C caseSimpleBlockStatement(TCSimpleBlockStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCStatement assignment: node.statements)
		{
			all.addAll(assignment.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSkipStatement(TCSkipStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseSpecificationStatement(TCSpecificationStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.precondition, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.postcondition, arg));
		
		if (node.externals != null)
		{
			for (TCExternalClause ex: node.externals)
			{
				all.addAll(visitorSet.applyTypeVisitor(ex.type, arg));
			}
		}
		
		if (node.errors != null)
		{
			for (TCErrorCase error: node.errors)
			{
				all.addAll(visitorSet.applyExpressionVisitor(error.left, arg));
				all.addAll(visitorSet.applyExpressionVisitor(error.right, arg));
			}
		}

		return all;
	}

 	@Override
	public C caseSporadicStatement(TCSporadicStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseStartStatement(TCStartStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.objects, arg);
	}

 	@Override
	public C caseStopStatement(TCStopStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.objects, arg);
	}

 	@Override
	public C caseSubclassResponsibilityStatement(TCSubclassResponsibilityStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseTixeStatement(TCTixeStatement node, S arg)
	{
		C all = newCollection();
		
		for (TCTixeStmtAlternative alternative: node.traps)
		{
			all.addAll(visitorSet.applyPatternVisitor(alternative.patternBind.pattern, arg));
			all.addAll(visitorSet.applyBindVisitor(alternative.patternBind.bind, arg));
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTrapStatement(TCTrapStatement node, S arg)
	{
		C all = visitorSet.applyPatternVisitor(node.patternBind.pattern, arg);
		all.addAll(visitorSet.applyBindVisitor(node.patternBind.bind, arg));
		all.addAll(node.with.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseWhileStatement(TCWhileStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.exp, arg);
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

	@Override
	public C caseTraceVariableStatement(TCTraceVariableStatement node, S arg)
	{
		return newCollection();
	}

	private C caseStateDesignator(TCStateDesignator designator, S arg)
	{
		if (designator instanceof TCFieldDesignator)
		{
			TCFieldDesignator fd = (TCFieldDesignator)designator;
			return caseStateDesignator(fd.object, arg);
		}
		else if (designator instanceof TCIdentifierDesignator)
		{
			return newCollection();
		}
		else if (designator instanceof TCMapSeqDesignator)
		{
			TCMapSeqDesignator msd = (TCMapSeqDesignator)designator;
			C all = caseStateDesignator(msd.mapseq, arg);
			all.addAll(visitorSet.applyExpressionVisitor(msd.exp, arg));
			return all;
		}
		else
		{
			throw new IllegalArgumentException("caseStateDesignator");
		}
	}

	private C caseObjectDesignator(TCObjectDesignator designator, S arg)
	{
		if (designator instanceof TCObjectApplyDesignator)
		{
			TCObjectApplyDesignator ad = (TCObjectApplyDesignator)designator;
			C all = caseObjectDesignator(ad.object, arg);
			
			for (TCExpression exp: ad.args)
			{
				all.addAll(visitorSet.applyExpressionVisitor(exp, arg));
			}
			
			return all;
		}
		else if (designator instanceof TCObjectFieldDesignator)
		{
			TCObjectFieldDesignator fd = (TCObjectFieldDesignator)designator;
			return caseObjectDesignator(fd.object, arg);
		}
		else if (designator instanceof TCObjectNewDesignator)
		{
			TCObjectNewDesignator nd = (TCObjectNewDesignator)designator;
			return visitorSet.applyExpressionVisitor(nd.expression, arg);
		}
		else if (designator instanceof TCObjectIdentifierDesignator)
		{
			return newCollection();
		}
		else if (designator instanceof TCObjectSelfDesignator)
		{
			return newCollection();
		}
		else
		{
			throw new IllegalArgumentException("caseObjectDesignator");
		}
	}

	abstract protected C newCollection();
}
