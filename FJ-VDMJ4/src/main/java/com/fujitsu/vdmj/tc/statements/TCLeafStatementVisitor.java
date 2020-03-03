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

package com.fujitsu.vdmj.tc.statements;

import java.util.Collection;

import com.fujitsu.vdmj.tc.annotations.TCAnnotatedStatement;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleSetBind;
import com.fujitsu.vdmj.tc.patterns.TCSeqBind;
import com.fujitsu.vdmj.tc.patterns.TCSetBind;
import com.fujitsu.vdmj.tc.traces.TCTraceVariableStatement;

/**
 * This TCStatementVisitor visits all of the leaves of an statement tree and calls
 * the basic processing methods for the simple statements.
 */
abstract public class TCLeafStatementVisitor<E, C extends Collection<E>, S> extends TCStatementVisitor<C, S>
{
 	@Override
	public C caseAnnotatedStatement(TCAnnotatedStatement node, S arg)
	{
 		return node.statement.apply(this, arg);
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
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.exp.apply(expVisitor, arg) : newCollection());
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
		
		for (TCStatement statement: node.statements)
		{
			all.addAll(statement.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCallObjectStatement(TCCallObjectStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (TCExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseCallStatement(TCCallStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (TCExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
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
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.cycles.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseDurationStatement(TCDurationStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.duration.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElseIfStatement(TCElseIfStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.elseIfExp.apply(expVisitor, arg));
		}
		
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
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseForAllStatement(TCForAllStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.set.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForIndexStatement(TCForIndexStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
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
	public C caseForPatternBindStatement(TCForPatternBindStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = caseBind(node.patternBind.bind, arg);
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIfStatement(TCIfStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.ifExp.apply(expVisitor, arg));
		}
		
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
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = caseMultipleBind(node.bind, arg);
		
		if (expVisitor != null && node.suchThat != null)
		{
			all.addAll(node.suchThat.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetDefStatement(TCLetDefStatement node, S arg)
	{
		C all = newCollection();
		
		// TODO defs!
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C casePeriodicStatement(TCPeriodicStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (TCExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseReturnStatement(TCReturnStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
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
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
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
	public C caseSporadicStatement(TCSporadicStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (TCExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseStartStatement(TCStartStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.objects.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseStopStatement(TCStopStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.objects.apply(expVisitor, arg) : newCollection());
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
		C all = caseBind(node.patternBind.bind, arg);
		all.addAll(node.with.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseWhileStatement(TCWhileStatement node, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

	@Override
	public C caseTraceVariableStatement(TCTraceVariableStatement node, S arg)
	{
		return newCollection();
	}

	private C caseBind(TCBind bind, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof TCSetBind)
			{
				TCSetBind sbind = (TCSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof TCSeqBind)
			{
				TCSeqBind sbind = (TCSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	private C caseMultipleBind(TCMultipleBind bind, S arg)
	{
		TCLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
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
	
	abstract protected C newCollection();

 	abstract protected TCLeafExpressionVisitor<E, C, S> getExpressionVisitor();
}
