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

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.annotations.POLoopAnnotations;
import com.fujitsu.vdmj.po.annotations.POLoopInvariantList;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POLocalDefinition;
import com.fujitsu.vdmj.po.expressions.POAndExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POInSetExpression;
import com.fujitsu.vdmj.po.expressions.POProperSubsetExpression;
import com.fujitsu.vdmj.po.expressions.POSetDifferenceExpression;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSetUnionExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAltContext;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POCommentContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class POForAllStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POPattern pattern;
	public final POExpression set;
	public final POStatement statement;
	public final POLoopAnnotations invariants;

	public POForAllStatement(LexLocation location,
		POPattern pattern, POExpression set, POStatement stmt, POLoopAnnotations invariants)
	{
		super(location);
		this.pattern = pattern;
		this.set = set;
		this.statement = stmt;
		this.invariants = invariants;
	}

	@Override
	public String toString()
	{
		return "for all " + pattern + " in set " + set + " do\n" + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		pogState.setAmbiguous(false);
		POExpression eset = extractOpCalls(set, obligations, pogState, ctxt, env);
		obligations.addAll(eset.getProofObligations(ctxt, pogState, env));

		boolean varAmbiguous = pogState.isAmbiguous();
		POLoopInvariantList annotations = invariants.getList();
		TCNameSet updates = statement.updatesState(ctxt);
		POExpression invariant = null;
		
		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(true);
		}
	
		POAssignmentDefinition ghostDef = annotations.getGhostDef();
		TCNameToken ghostName = ghostDef.name;
		POAltContext altCtxt = new POAltContext();

		if (varAmbiguous)
		{
			ctxt.push(new POAmbiguousContext("loop var", pattern.getVariableNames(), location));
		}

		if (invariant == null)
		{
			ctxt.push(new POCommentContext("Missing @LoopInvariant, assuming true", location));
			PODefinition loopinv = getLoopInvDef();
			ctxt.push(new POLetDefContext(loopinv));
			invariant = new POVariableExpression(loopinv.name, loopinv);
		}

		int popto = ctxt.size();	// Includes missing invariant above

		/**
		 * The initial case verifies that the invariant is true for the empty ax/gx state.
		 */
		ctxt.push(new POLetDefContext(ghostDef));		// let ghost = {} in
		obligations.addAll(LoopInvariantObligation.getAllPOs(invariant.location, ctxt, invariant));
		obligations.lastElement().setMessage("check invariant before for-loop");
		ctxt.pop();

		/**
		 * The preservation case verifies that if invariant is true for gx, then it is true for gx union {x}
		 */
		TCSetType stype = eset.getExptype().getSet();
		PODefinitionList podefs = pattern.getDefinitions(stype.setof);
		TCDefinitionList tcdefs = new TCDefinitionList();

		for (PODefinition podef: podefs)
		{
			if (podef instanceof POLocalDefinition)		// pattern defs will be local
			{
				tcdefs.add(new TCLocalDefinition(location, podef.name, podef.getType()));
			}
		}

		tcdefs.add(new TCLocalDefinition(location, ghostName, ghostDef.type));
		Environment local = new FlatCheckedEnvironment(tcdefs, env, NameScope.NAMES);
		updates.addAll(pattern.getVariableNames());
		updates.add(ghostName);

		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(false);	// Don't exclude loop vars now
		}

		ctxt.push(new POForAllContext(updates, local));							// forall <changed values> and vars
		ctxt.push(new POImpliesContext(varsInSet(ghostDef, eset), invariant));	// x in set S \ GHOST$ && invariant => ...
		ctxt.push(new POLetDefContext(ghostUpdate(ghostDef)));					// ghost := ghost union {x}

		obligations.addAll(statement.getProofObligations(ctxt, pogState, env));
		obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, invariant));
		obligations.lastElement().setMessage("invariant preservation for next for-loop");

		/**
		 * The context stack now contains everything from the statement block, but we want to
		 * suppress this, since context beyond the loop only includes the invariant. But we can't just
		 * discard it, because it may include some returns, which are subsequently needed by
		 * postcondition checks. So we extract the substack, and reduce it to just the paths with
		 * returnsEarly().
		 */
		POContextStack stack = new POContextStack();
		ctxt.popInto(popto, stack);
		altCtxt.addAll(stack.reduce());
		
		/**
		 * Leave implication for following POs, which uses the LoopInvariants that exclude "vars",
		 * and GHOST$ set to the original set value.
		 */
		updates.remove(ghostName);
		updates.removeAll(pattern.getVariableNames());

		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(true);
		}

		ctxt.push(new POLetDefContext(ghostFinal(ghostDef, eset)));	// let GHOST$ = set in
		ctxt.push(new POForAllContext(updates, env));				// forall <changed variables>
		ctxt.push(new POImpliesContext(invariant));					// invariant => ...
		ctxt.popInto(popto, altCtxt.add());

		// The two alternatives in one added.
		ctxt.push(altCtxt);

		return obligations;
	}

	/**
	 * Produce "ghost := ghost union {x}"
	 */
	private POAssignmentDefinition ghostUpdate(POAssignmentDefinition ghostDef)
	{
		POLocalDefinition vardef = new POLocalDefinition(location, ghostDef.name, ghostDef.type);
		POExpressionList elist = new POExpressionList();
		elist.add(pattern.getMatchingExpression());
		TCTypeList tlist = new TCTypeList();
		tlist.add(ghostDef.type);

		POSetUnionExpression union = new POSetUnionExpression(
			new POVariableExpression(ghostDef.name, vardef),
			new LexKeywordToken(Token.UNION, location),
			new POSetEnumExpression(location, elist, tlist), ghostDef.type, ghostDef.type);

		return new POAssignmentDefinition(ghostDef.name, ghostDef.type, union, ghostDef.type);
	}

	/**
	 * Produce "ghost := <set>"
	 */
	private POAssignmentDefinition ghostFinal(POAssignmentDefinition ghostDef, POExpression eset)
	{
		return new POAssignmentDefinition(ghostDef.name, ghostDef.type, eset, ghostDef.type);
	}

	/**
	 * Produce "(ghost subset values) and x in set (values \ ghost)"
	 */
	private POExpression varsInSet(POAssignmentDefinition ghostDef, POExpression eset)
	{
		POLocalDefinition vardef = new POLocalDefinition(location, ghostDef.name, ghostDef.type);
		TCType setof = ghostDef.type.getSet().setof;
		TCBooleanType boolt = new TCBooleanType(location);

		return new POAndExpression(
			new POProperSubsetExpression(
				new POVariableExpression(ghostDef.name, vardef),
				new LexKeywordToken(Token.PSUBSET, location),
				eset,
				setof, setof),

			new LexKeywordToken(Token.AND, location),
		
			new POInSetExpression(
				pattern.getMatchingExpression(),				// eg mk_(x, y)
				new LexKeywordToken(Token.INSET, location),
				new POSetDifferenceExpression(
					eset,
					new LexKeywordToken(Token.SETDIFF, location),
					new POVariableExpression(ghostDef.name, vardef), ghostDef.type, ghostDef.type),
				setof, ghostDef.type),

			boolt, boolt);
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForAllStatement(this, arg);
	}
}
