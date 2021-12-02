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
import com.fujitsu.vdmj.tc.statements.TCDurationStatement;
import com.fujitsu.vdmj.tc.statements.TCElseIfStatement;
import com.fujitsu.vdmj.tc.statements.TCErrorStatement;
import com.fujitsu.vdmj.tc.statements.TCExitStatement;
import com.fujitsu.vdmj.tc.statements.TCForAllStatement;
import com.fujitsu.vdmj.tc.statements.TCForIndexStatement;
import com.fujitsu.vdmj.tc.statements.TCForPatternBindStatement;
import com.fujitsu.vdmj.tc.statements.TCIfStatement;
import com.fujitsu.vdmj.tc.statements.TCLetBeStStatement;
import com.fujitsu.vdmj.tc.statements.TCLetDefStatement;
import com.fujitsu.vdmj.tc.statements.TCPeriodicStatement;
import com.fujitsu.vdmj.tc.statements.TCReturnStatement;
import com.fujitsu.vdmj.tc.statements.TCSimpleBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCSkipStatement;
import com.fujitsu.vdmj.tc.statements.TCSpecificationStatement;
import com.fujitsu.vdmj.tc.statements.TCSporadicStatement;
import com.fujitsu.vdmj.tc.statements.TCStartStatement;
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
		C all = newCollection();
		all.addAll(node.always.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseAssignmentStatement(TCAssignmentStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
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
		C all = newCollection();
		
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
		C all = newCollection();
		
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
 		if (node.expression != null)
 		{
			return visitorSet.applyExpressionVisitor(node.expression, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseForAllStatement(TCForAllStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.set, arg);
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForIndexStatement(TCForIndexStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.from, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.to, arg));
		
		if (node.by != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.by, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForPatternBindStatement(TCForPatternBindStatement node, S arg)
	{
		C all = newCollection();
		
		if (node.patternBind.bind != null)
		{
			visitorSet.applyBindVisitor(node.patternBind.bind, arg);
		}
		
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
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
			all.addAll(node.elseStmt.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseLetBeStStatement(TCLetBeStStatement node, S arg)
	{
		C all = visitorSet.applyMultiBindVisitor(node.bind, arg);
		
		if (node.suchThat != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.suchThat, arg));
		}
		
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
 		if (node.expression != null)
 		{
			return visitorSet.applyExpressionVisitor(node.expression, arg);
 		}
 		else
 		{
 			return newCollection();
 		}
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
		C all = newCollection();
		
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
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTrapStatement(TCTrapStatement node, S arg)
	{
		C all = newCollection();
		
		if (node.patternBind.bind != null)
		{
			visitorSet.applyBindVisitor(node.patternBind.bind, arg);
		}
		
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

	abstract protected C newCollection();
}
