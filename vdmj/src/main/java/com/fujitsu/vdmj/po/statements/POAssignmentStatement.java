/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POAssignmentContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POResolveContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.StateInvariantObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class POAssignmentStatement extends POStatement
{
	private static final long serialVersionUID = 1L;

	public final POExpression exp;
	public final POStateDesignator target;
	public final TCType targetType;
	public final TCType expType;
	public final POClassDefinition classDefinition;
	public final POStateDefinition stateDefinition;
	public final boolean inConstructor;

	public POAssignmentStatement(LexLocation location, POStateDesignator target, POExpression exp,
		TCType targetType, TCType expType, POClassDefinition classDefinition,
		POStateDefinition stateDefinition, boolean inConstructor)
	{
		super(location);
		this.exp = exp;
		this.target = target;
		this.targetType = targetType;
		this.expType = expType;
		this.classDefinition = classDefinition;
		this.stateDefinition = stateDefinition;
		this.inConstructor = inConstructor;
	}

	@Override
	public String toString()
	{
		return target + " := " + exp;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		pogState.setAmbiguous(false);

		// Attempt to extract operation calls from the LHS and RHS
		POStateDesignator designator = extractOpCalls(target, obligations, pogState, ctxt, env);
		POExpression extracted = extractOpCalls(exp, obligations, pogState, ctxt, env);

		obligations.addAll(designator.getProofObligations(ctxt));
		obligations.addAll(extracted.getProofObligations(ctxt, pogState, env));

		if (!TypeComparator.isSubType(ctxt.checkType(exp, expType), targetType))
		{
			obligations.addAll(
				SubTypeObligation.getAllPOs(extracted, targetType, expType, ctxt));
		}
		
		TCNameToken update = designator.updatedVariableName();
		
		// If the expression is unambiguous, we can disambiguate the state being updated.
		
		if (!pogState.isAmbiguous())
		{
			ctxt.push(new POAssignmentContext(designator, targetType, extracted));
			
			// We can disambiguate variables in an assignment that assigns unambiguous values,
			// like constants or variables that are unambiguous, but only if the entire value
			// is being replaced. So we check that we are assigning to an IdentifierDesignator.
			
			if (designator instanceof POIdentifierDesignator && ctxt.isAmbiguous(update))
			{
				ctxt.push(new POResolveContext(update, location));
			}
		}
		else
		{
			// Updated a variable with an ambiguous value, so it becomes ambiguous
			ctxt.push(new POAmbiguousContext("assignment", new TCNameList(update), exp.location));
		}

		if (!inConstructor &&
			(classDefinition != null && classDefinition.invariant != null) ||
			(stateDefinition != null && stateDefinition.invExpression != null))
		{
			obligations.addAll(StateInvariantObligation.getAllPOs(this, ctxt));
		}

		return obligations;
	}
	
	/**
	 * Prepare an assignment during an "atomic" - see POAtomicStatement.
	 */
	public ProofObligationList prepareAssignment(POContextStack ctxt, POGState pogState, Environment env, int var)
	{
		ProofObligationList obligations = new ProofObligationList();

		obligations.addAll(target.getProofObligations(ctxt));
		obligations.addAll(exp.getProofObligations(ctxt, pogState, env));

		if (!TypeComparator.isSubType(ctxt.checkType(exp, expType), targetType))
		{
			obligations.addAll(
				SubTypeObligation.getAllPOs(exp, targetType, expType, ctxt));
		}
		
		// Create a temporary name, which is used in the completeObligations call
		TCNameToken temp = new TCNameToken(location, location.module, "$atomic" + var);
		POIdentifierDesignator tempTarget = new POIdentifierDesignator(temp, null);
		
		ctxt.push(new POAssignmentContext(tempTarget, targetType, exp));

		if (ctxt.hasAmbiguous(exp.getVariableNames()))
		{
			// Updated a variable with an ambiguous value, so it becomes ambiguous
			TCNameToken update = target.updatedVariableName();
			ctxt.push(new POAmbiguousContext("assignment", new TCNameList(update), exp.location));
		}

		return obligations;
	}

	/**
	 * Complete an assignment during an "atomic" - see POAtomicStatement.
	 */
	public ProofObligationList completeAssignment(POContextStack ctxt, POGState pogState, Environment env, int var)
	{
		ProofObligationList obligations = new ProofObligationList();
		
		// Create a temporary name, which was created in the completeObligations call
		TCNameToken temp = new TCNameToken(location, location.module, "$atomic" + var);
		POVariableExpression tempExp = new POVariableExpression(temp, null);
		ctxt.push(new POAssignmentContext(target, targetType, tempExp));

		return obligations;
	}
	
	/**
	 * Check invariant holds after an "atomic" - see POAtomicStatement.
	 */
	public ProofObligationList checkInvariant(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();
		
		if (!inConstructor &&
			(classDefinition != null && classDefinition.invariant != null) ||
			(stateDefinition != null && stateDefinition.invExpression != null))
		{
			obligations.addAll(StateInvariantObligation.getAllPOs(this, ctxt));
		}
		
		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAssignmentStatement(this, arg);
	}
}
