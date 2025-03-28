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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAssignmentContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.StateInvariantObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
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

		obligations.addAll(target.getProofObligations(ctxt));
		obligations.addAll(exp.getProofObligations(ctxt, pogState, env).markIfAmbiguous(pogState, exp));

		if (!TypeComparator.isSubType(ctxt.checkType(exp, expType), targetType))
		{
			obligations.addAll(
				SubTypeObligation.getAllPOs(exp, targetType, expType, ctxt));
		}
		
		TCNameToken update = POStateDesignator.updatedVariableName(target);
		pogState.didUpdateState(update, location);
		
		TCNameSet varlist = exp.getVariableNames();		// All
		boolean isSimple = (target instanceof POIdentifierDesignator);
		
		if (target instanceof POMapSeqDesignator)
		{
			POMapSeqDesignator ms = (POMapSeqDesignator)target;
			varlist.addAll(ms.exp.getVariableNames());	// eg. add "x" in m(x)
		}
		
		if (!pogState.hasAmbiguousState(varlist))
		{
			ctxt.push(new POAssignmentContext(target, targetType, exp));
			
			// We can disambiguate variables in a simple assignment that assigns unambiguous values,
			// like constants or variables that are unambiguous.
			
			if (isSimple)	// Other elements of complex designators may be ambiguous
			{
				pogState.notAmbiguous(update);
			}
		}
		else
		{
			// Updated a variable with an ambiguous value, so it becomes ambiguous
			pogState.isAmbiguous(update, exp.location);
		}

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
