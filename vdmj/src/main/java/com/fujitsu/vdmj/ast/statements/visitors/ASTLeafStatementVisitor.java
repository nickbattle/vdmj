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

package com.fujitsu.vdmj.ast.statements.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.ast.ASTVisitorSet;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.statements.ASTAlwaysStatement;
import com.fujitsu.vdmj.ast.statements.ASTAssignmentStatement;
import com.fujitsu.vdmj.ast.statements.ASTAtomicStatement;
import com.fujitsu.vdmj.ast.statements.ASTBlockStatement;
import com.fujitsu.vdmj.ast.statements.ASTCallObjectStatement;
import com.fujitsu.vdmj.ast.statements.ASTCallStatement;
import com.fujitsu.vdmj.ast.statements.ASTCaseStmtAlternative;
import com.fujitsu.vdmj.ast.statements.ASTCasesStatement;
import com.fujitsu.vdmj.ast.statements.ASTCyclesStatement;
import com.fujitsu.vdmj.ast.statements.ASTDurationStatement;
import com.fujitsu.vdmj.ast.statements.ASTElseIfStatement;
import com.fujitsu.vdmj.ast.statements.ASTErrorStatement;
import com.fujitsu.vdmj.ast.statements.ASTExitStatement;
import com.fujitsu.vdmj.ast.statements.ASTForAllStatement;
import com.fujitsu.vdmj.ast.statements.ASTForIndexStatement;
import com.fujitsu.vdmj.ast.statements.ASTForPatternBindStatement;
import com.fujitsu.vdmj.ast.statements.ASTIfStatement;
import com.fujitsu.vdmj.ast.statements.ASTLetBeStStatement;
import com.fujitsu.vdmj.ast.statements.ASTLetDefStatement;
import com.fujitsu.vdmj.ast.statements.ASTPeriodicStatement;
import com.fujitsu.vdmj.ast.statements.ASTReturnStatement;
import com.fujitsu.vdmj.ast.statements.ASTSimpleBlockStatement;
import com.fujitsu.vdmj.ast.statements.ASTSkipStatement;
import com.fujitsu.vdmj.ast.statements.ASTSpecificationStatement;
import com.fujitsu.vdmj.ast.statements.ASTSporadicStatement;
import com.fujitsu.vdmj.ast.statements.ASTStartStatement;
import com.fujitsu.vdmj.ast.statements.ASTStatement;
import com.fujitsu.vdmj.ast.statements.ASTStopStatement;
import com.fujitsu.vdmj.ast.statements.ASTSubclassResponsibilityStatement;
import com.fujitsu.vdmj.ast.statements.ASTTixeStatement;
import com.fujitsu.vdmj.ast.statements.ASTTixeStmtAlternative;
import com.fujitsu.vdmj.ast.statements.ASTTrapStatement;
import com.fujitsu.vdmj.ast.statements.ASTWhileStatement;

/**
 * This ASTStatement visitor visits all of the leaves of an statement tree and calls
 * the basic processing methods for the simple statements.
 */
abstract public class ASTLeafStatementVisitor<E, C extends Collection<E>, S> extends ASTStatementVisitor<C, S>
{
	protected ASTVisitorSet<E, C, S> visitorSet = new ASTVisitorSet<E, C, S>()
	{
		@Override
		protected void setVisitors()
		{
			statementVisitor = ASTLeafStatementVisitor.this;
		}

		@Override
		protected C newCollection()
		{
			return ASTLeafStatementVisitor.this.newCollection();
		}
	};

	@Override
	public C caseAlwaysStatement(ASTAlwaysStatement node, S arg)
	{
		C all = newCollection();
		all.addAll(node.always.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseAssignmentStatement(ASTAssignmentStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.exp, arg);
	}

	@Override
	public C caseAtomicStatement(ASTAtomicStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTAssignmentStatement assignment: node.assignments)
		{
			all.addAll(assignment.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseBlockStatement(ASTBlockStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTDefinition def: node.assignmentDefs)
		{
			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
		}
		
		for (ASTStatement statement: node.statements)
		{
			all.addAll(statement.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseCallObjectStatement(ASTCallObjectStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCallStatement(ASTCallStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCasesStatement(ASTCasesStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTCaseStmtAlternative alternative: node.cases)
		{
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCyclesStatement(ASTCyclesStatement node, S arg)
	{
		C all = newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.cycles, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseDurationStatement(ASTDurationStatement node, S arg)
	{
		C all = newCollection();
		all.addAll(visitorSet.applyExpressionVisitor(node.duration, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElseIfStatement(ASTElseIfStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.elseIfExp, arg);
		all.addAll(node.thenStmt.apply(this, arg));
		return all;
	}

 	@Override
	public C caseErrorStatement(ASTErrorStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseExitStatement(ASTExitStatement node, S arg)
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
	public C caseForAllStatement(ASTForAllStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.set, arg);
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForIndexStatement(ASTForIndexStatement node, S arg)
	{
		C all = newCollection();
		
		all.addAll(visitorSet.applyExpressionVisitor(node.from, arg));
		all.addAll(visitorSet.applyExpressionVisitor(node.to, arg));
		
		if (node.by != null)
		{
			all.addAll(visitorSet.applyExpressionVisitor(node.by, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForPatternBindStatement(ASTForPatternBindStatement node, S arg)
	{
 		C all = visitorSet.applyBindVisitor(node.patternBind.bind, arg);
		all.addAll(visitorSet.applyExpressionVisitor(node.exp, arg));
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIfStatement(ASTIfStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.ifExp, arg);
		all.addAll(node.thenStmt.apply(this, arg));
		
		if (node.elseList != null)
		{
			for (ASTElseIfStatement elseStmt: node.elseList)
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
	public C caseLetBeStStatement(ASTLetBeStStatement node, S arg)
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
	public C caseLetDefStatement(ASTLetDefStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTDefinition def: node.localDefs)
		{
			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C casePeriodicStatement(ASTPeriodicStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseReturnStatement(ASTReturnStatement node, S arg)
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
	public C caseSimpleBlockStatement(ASTSimpleBlockStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTStatement assignment: node.statements)
		{
			all.addAll(assignment.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSkipStatement(ASTSkipStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseSpecificationStatement(ASTSpecificationStatement node, S arg)
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
	public C caseSporadicStatement(ASTSporadicStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTExpression a: node.args)
		{
			all.addAll(visitorSet.applyExpressionVisitor(a, arg));
		}
		
		return all;
	}

 	@Override
	public C caseStartStatement(ASTStartStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.objects, arg);
	}

 	@Override
	public C caseStopStatement(ASTStopStatement node, S arg)
	{
		return visitorSet.applyExpressionVisitor(node.objects, arg);
	}

 	@Override
	public C caseSubclassResponsibilityStatement(ASTSubclassResponsibilityStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseTixeStatement(ASTTixeStatement node, S arg)
	{
		C all = newCollection();
		
		for (ASTTixeStmtAlternative alternative: node.traps)
		{
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTrapStatement(ASTTrapStatement node, S arg)
	{
		C all = visitorSet.applyBindVisitor(node.patternBind.bind, arg);
		all.addAll(node.with.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseWhileStatement(ASTWhileStatement node, S arg)
	{
		C all = visitorSet.applyExpressionVisitor(node.exp, arg);
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

	abstract protected C newCollection();
}
