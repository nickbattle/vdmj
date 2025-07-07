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

import java.util.List;

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.annotations.POLoopInvariantAnnotation;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POLocalDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POInSetExpression;
import com.fujitsu.vdmj.po.expressions.POSetDifferenceExpression;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;

public class POForAllStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POPattern pattern;
	public final POExpression set;
	public final POStatement statement;

	public POForAllStatement(LexLocation location,
		POPattern pattern, POExpression set, POStatement stmt)
	{
		super(location);
		this.pattern = pattern;
		this.set = set;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "for all " + pattern + " in set " + set + " do\n" + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = set.getProofObligations(ctxt, pogState, env);

		List<POLoopInvariantAnnotation> invariants = annotations.getInstances(POLoopInvariantAnnotation.class);
		TCNameSet updates = statement.updatesState();
		
		if (invariants.isEmpty())		// No loop invariants defined
		{
			int popto = ctxt.pushAt(new POForAllSequenceContext(pattern, set, " in set "));
			ProofObligationList loops = statement.getProofObligations(ctxt, pogState, env);
			ctxt.popTo(popto);
			
			if (statement.getStmttype().hasReturn())
			{
				updates.add(TCNameToken.getResult(location));
			}
			
			if (!updates.isEmpty())
			{
				ctxt.push(new POAmbiguousContext("for all loop", updates, location));
			}
			
			obligations.addAll(loops);
			return obligations;
		}
		else
		{
			POExpression invariant = POLoopInvariantAnnotation.combine(invariants, null);
			POAssignmentDefinition ghost = ghostDef();

			/*
			 * The initial case verifies that the invariant is true for the empty ax/gx state.
			 */
			ctxt.add(new POLetDefContext(ghost));		// let ghost = {} in
			obligations.addAll(LoopInvariantObligation.getAllPOs(invariant.location, ctxt, invariant));
			obligations.lastElement().setMessage("check before for-loop");

			int popto = ctxt.size();

			/*
			 * A preservation case verifies that if invariant is true for gx, then it is true for gx union {x}
			 */
			TCSetType stype = set.getExptype().getSet();
			PODefinitionList podefs = pattern.getDefinitions(stype.setof);
			TCDefinitionList tcdefs = new TCDefinitionList();

			for (PODefinition podef: podefs)
			{
				if (podef instanceof POLocalDefinition)		// pattern defs will be local
				{
					tcdefs.add(new TCLocalDefinition(location, podef.name, podef.getType()));
				}
			}

			Environment local = new FlatCheckedEnvironment(tcdefs, env, null);
			updates.addAll(pattern.getVariableNames());

			ctxt.push(new POForAllContext(updates, local));					// forall <changed values> and vars
			ctxt.push(new POImpliesContext(varsInSet(ghost), invariant));	// x in set S && invariant => ...
			obligations.addAll(statement.getProofObligations(ctxt, pogState, env));

			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, invariant));
			obligations.lastElement().setMessage("preservation for next for-loop");

			ctxt.popTo(popto);


/***
			// Note: location of initial check is the @LoopInvariant itself.
			obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check before for-loop");

			int popto = ctxt.size();
			
			ctxt.push(new POForAllSequenceContext(pattern, set, " in set "));	// forall p in set S
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check before each for-loop");
			
			if (!updates.isEmpty())	ctxt.push(new POForAllContext(updates, env));		// forall <changed variables>
			ctxt.push(new POImpliesContext(annotation.invariant));						// invariant => ...
			obligations.addAll(statement.getProofObligations(ctxt, pogState, env));
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check after each for-loop");

			ctxt.popTo(popto);
			
			// Leave implication for following POs
			if (!updates.isEmpty()) ctxt.push(new POForAllContext(updates, env));		// forall <changed variables>
			ctxt.push(new POImpliesContext(annotation.invariant));						// invariant => ...
			
***/
			return obligations;
		}
	}

	private POAssignmentDefinition ghostDef()
	{
		TCNameToken GHOST = new TCNameToken(location, location.module, "GHOST$");
		TCSetType stype = set.getExptype().getSet();

		if (stype instanceof TCSet1Type)
		{
			stype = new TCSetType(location, stype.setof);	// can't be set1 for ghost
		}

		POSetEnumExpression empty = new POSetEnumExpression(location, new POExpressionList(), new TCTypeList());
		return new POAssignmentDefinition(GHOST, stype, empty, stype);
	}

	/**
	 * Produce x subset (ax \ gx)
	 */
	private POExpression varsInSet(POAssignmentDefinition ghost)
	{
		TCSetType stype = set.getExptype().getSet();
		
		return new POInSetExpression(
			pattern.getMatchingExpression(),				// eg mk_(x, y)
			new LexKeywordToken(Token.INSET, location),
			new POSetDifferenceExpression(
				set,
				new LexKeywordToken(Token.SETDIFF, location),
				ghost.expression, stype, stype),
			stype.setof, stype);
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForAllStatement(this, arg);
	}
}
