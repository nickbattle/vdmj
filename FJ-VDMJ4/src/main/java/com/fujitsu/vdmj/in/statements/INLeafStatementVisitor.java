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

package com.fujitsu.vdmj.in.statements;

import java.util.Collection;

import com.fujitsu.vdmj.in.annotations.INAnnotatedStatement;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INLeafDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INLeafExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleSeqBind;
import com.fujitsu.vdmj.in.patterns.INMultipleSetBind;
import com.fujitsu.vdmj.in.patterns.INSeqBind;
import com.fujitsu.vdmj.in.patterns.INSetBind;

/**
 * This INStatement visitor visits all of the leaves of an statement tree and calls
 * the basic processing methods for the simple statements.
 */
abstract public class INLeafStatementVisitor<E, C extends Collection<E>, S> extends INStatementVisitor<C, S>
{
 	@Override
	public C caseAnnotatedStatement(INAnnotatedStatement node, S arg)
	{
 		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
 		C all = newCollection();
 		
 		if (expVisitor != null)
 		{
	 		for (INExpression a: node.annotation.args)
	 		{
	 			all.addAll(a.apply(expVisitor, arg));
	 		}
 		}
 		
 		all.addAll(node.statement.apply(this, arg));
 		return all;
	}

	@Override
	public C caseAlwaysStatement(INAlwaysStatement node, S arg)
	{
		C all = newCollection();
		all.addAll(node.always.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseAssignmentStatement(INAssignmentStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.exp.apply(expVisitor, arg) : newCollection());
	}

	@Override
	public C caseAtomicStatement(INAtomicStatement node, S arg)
	{
		C all = newCollection();
		
		for (INAssignmentStatement assignment: node.assignments)
		{
			all.addAll(assignment.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseBlockStatement(INBlockStatement node, S arg)
	{
		INLeafDefinitionVisitor<E, C, S> defVisitor = getDefinitionVisitor();
		C all = newCollection();
		
		if (defVisitor != null)
		{
			for (INDefinition def: node.assignmentDefs)
			{
				all.addAll(def.apply(defVisitor, arg));
			}
		}
		
		for (INStatement statement: node.statements)
		{
			all.addAll(statement.apply(this, arg));
		}
		
		return all;
	}

	@Override
	public C caseCallObjectStatement(INCallObjectStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (INExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseCallStatement(INCallStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (INExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseCasesStatement(INCasesStatement node, S arg)
	{
		C all = newCollection();
		
		for (INCaseStmtAlternative alternative: node.cases)
		{
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseCyclesStatement(INCyclesStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.cycles.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseDurationStatement(INDurationStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.duration.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseElseIfStatement(INElseIfStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.elseIfExp.apply(expVisitor, arg));
		}
		
		all.addAll(node.thenStmt.apply(this, arg));
		return all;
	}

 	@Override
	public C caseErrorStatement(INErrorStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseExitStatement(INExitStatement node, S arg)
	{
 		if (node.expression != null)
 		{
			INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
			return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseForAllStatement(INForAllStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.set.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseForIndexStatement(INForIndexStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
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
	public C caseForPatternBindStatement(INForPatternBindStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = caseBind(node.patternBind.bind, arg);
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseIfStatement(INIfStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.ifExp.apply(expVisitor, arg));
		}
		
		all.addAll(node.thenStmt.apply(this, arg));
		
		if (node.elseList != null)
		{
			for (INElseIfStatement elseStmt: node.elseList)
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
	public C caseLetBeStStatement(INLetBeStStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = caseMultipleBind(node.bind, arg);
		
		if (expVisitor != null && node.suchThat != null)
		{
			all.addAll(node.suchThat.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C caseLetDefStatement(INLetDefStatement node, S arg)
	{
		INLeafDefinitionVisitor<E, C, S> defVisitor = getDefinitionVisitor();
		C all = newCollection();
		
		if (defVisitor != null)
		{
			for (INDefinition def: node.localDefs)
			{
				all.addAll(def.apply(defVisitor, arg));
			}
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

 	@Override
	public C casePeriodicStatement(INPeriodicStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (INExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseReturnStatement(INReturnStatement node, S arg)
	{
 		if (node.expression != null)
 		{
			INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
			return (expVisitor != null ? node.expression.apply(expVisitor, arg) : newCollection());
 		}
 		else
 		{
 			return newCollection();
 		}
	}

 	@Override
	public C caseSimpleBlockStatement(INSimpleBlockStatement node, S arg)
	{
		C all = newCollection();
		
		for (INStatement assignment: node.statements)
		{
			all.addAll(assignment.apply(this, arg));
		}
		
		return all;
	}

 	@Override
	public C caseSkipStatement(INSkipStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseSpecificationStatement(INSpecificationStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
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
	public C caseSporadicStatement(INSporadicStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			for (INExpression a: node.args)
			{
				all.addAll(a.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	@Override
	public C caseStartStatement(INStartStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.objects.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseStopStatement(INStopStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		return (expVisitor != null ? node.objects.apply(expVisitor, arg) : newCollection());
	}

 	@Override
	public C caseSubclassResponsibilityStatement(INSubclassResponsibilityStatement node, S arg)
	{
		return newCollection();
	}

 	@Override
	public C caseTixeStatement(INTixeStatement node, S arg)
	{
		C all = newCollection();
		
		for (INTixeStmtAlternative alternative: node.traps)
		{
			all.addAll(alternative.statement.apply(this, arg));
		}
		
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseTrapStatement(INTrapStatement node, S arg)
	{
		C all = caseBind(node.patternBind.bind, arg);
		all.addAll(node.with.apply(this, arg));
		all.addAll(node.body.apply(this, arg));
		return all;
	}

 	@Override
	public C caseWhileStatement(INWhileStatement node, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			all.addAll(node.exp.apply(expVisitor, arg));
		}
		
		all.addAll(node.statement.apply(this, arg));
		return all;
	}

	private C caseBind(INBind bind, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof INSetBind)
			{
				INSetBind sbind = (INSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof INSeqBind)
			{
				INSeqBind sbind = (INSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}

 	private C caseMultipleBind(INMultipleBind bind, S arg)
	{
		INLeafExpressionVisitor<E, C, S> expVisitor = getExpressionVisitor();
		C all = newCollection();
		
		if (expVisitor != null)
		{
			if (bind instanceof INMultipleSetBind)
			{
				INMultipleSetBind sbind = (INMultipleSetBind)bind;
				all.addAll(sbind.set.apply(expVisitor, arg));
			}
			else if (bind instanceof INMultipleSeqBind)
			{
				INMultipleSeqBind sbind = (INMultipleSeqBind)bind;
				all.addAll(sbind.sequence.apply(expVisitor, arg));
			}
		}
		
		return all;
	}
	
	abstract protected C newCollection();

 	abstract protected INLeafExpressionVisitor<E, C, S> getExpressionVisitor();

 	abstract protected INLeafDefinitionVisitor<E, C, S> getDefinitionVisitor();
}
