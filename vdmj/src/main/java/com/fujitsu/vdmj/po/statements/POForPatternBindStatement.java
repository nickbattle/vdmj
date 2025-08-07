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

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.annotations.POLoopAnnotations;
import com.fujitsu.vdmj.po.annotations.POLoopInvariantList;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POLocalDefinition;
import com.fujitsu.vdmj.po.expressions.POEqualsExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POIntegerLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POLenExpression;
import com.fujitsu.vdmj.po.expressions.POSeqConcatExpression;
import com.fujitsu.vdmj.po.expressions.POSeqEnumExpression;
import com.fujitsu.vdmj.po.expressions.POSubseqExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternBind;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAltContext;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POCommentContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqMemberObligation;
import com.fujitsu.vdmj.pog.SetMemberObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class POForPatternBindStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POPatternBind patternBind;
	public final boolean reverse;
	public final POExpression sequence;
	public final TCType expType;
	public final POStatement statement;
	public final POLoopAnnotations invariants;

	public POForPatternBindStatement(LexLocation location,
		POPatternBind patternBind, boolean reverse, POExpression exp, TCType expType,
		POStatement body, POLoopAnnotations invariants)
	{
		super(location);
		this.patternBind = patternBind;
		this.reverse = reverse;
		this.sequence = exp;
		this.expType = expType;
		this.statement = body;
		this.invariants = invariants;
	}

	@Override
	public String toString()
	{
		return "for " + patternBind + " in " +
			(reverse ? " reverse " : "") + sequence + " do\n" + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		pogState.setAmbiguous(false);
		ProofObligationList obligations = new ProofObligationList();

		POExpression eseq = extractOpCalls(sequence, obligations, pogState, ctxt, env);
		obligations.addAll(eseq.getProofObligations(ctxt, pogState, env));

		boolean varAmbiguous = pogState.isAmbiguous();
		POLoopInvariantList annotations = invariants.getList();
		TCNameSet updates = statement.updatesState();
		POExpression invariant = null;
		
		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(true);		// exclude loop firstly
		}

		POAssignmentDefinition ghostDef = invariants.getList().getGhostDef();
		POAltContext altCtxt = new POAltContext();

		if (varAmbiguous)
		{
			ctxt.push(new POAmbiguousContext("loop var", getPattern().getVariableNames(), location));
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
		 * The initial case verifies that the invariant is true for the empty ghost state.
		 */
		ctxt.push(new POLetDefContext(ghostDef));			// let ghost = [] in
		obligations.addAll(LoopInvariantObligation.getAllPOs(invariant.location, ctxt, invariant));
		obligations.lastElement().setMessage("check invariant before for-loop");
		ctxt.pop();

		if (patternBind.pattern != null)					// for p in [a,b,c] do
		{
			// nothing special
		}
		else if (patternBind.bind instanceof POTypeBind)	// for p : nat in [a,b,c] do
		{
			POTypeBind bind = (POTypeBind)patternBind.bind;
			TCSeqType s = expType.isSeq(location) ? expType.getSeq() : null;
			
			if (s != null && !TypeComparator.isSubType(s.seqof, bind.type))
			{
				ctxt.push(new POForAllSequenceContext(bind, eseq));
				obligations.addAll(SubTypeObligation.getAllPOs(
					bind.pattern.getMatchingExpression(), bind.type, s.seqof, ctxt));
				ctxt.pop();
			}
		}
		else if (patternBind.bind instanceof POSetBind)		// for p in set S in [a,b,c] do
		{
			POSetBind bind = (POSetBind)patternBind.bind;
			obligations.addAll(bind.set.getProofObligations(ctxt, pogState, env));

			ctxt.push(new POForAllSequenceContext(bind, eseq));
			obligations.addAll(SetMemberObligation.getAllPOs(
				bind.pattern.getMatchingExpression(), bind.set, ctxt));
			ctxt.pop();
		}
		else if (patternBind.bind instanceof POSeqBind)		// for p in seq S in [a,b,c] do
		{
			POSeqBind bind = (POSeqBind)patternBind.bind;
			obligations.addAll(bind.sequence.getProofObligations(ctxt, pogState, env));

			ctxt.push(new POForAllSequenceContext(bind, eseq));
			obligations.addAll(SeqMemberObligation.getAllPOs(
				bind.pattern.getMatchingExpression(), bind.sequence, ctxt));
			ctxt.pop();
		}

		/**
		 * The preservation case verifies that if invariant is true for gx, then it is true for gx ^ {x}
		 */
		TCSeqType stype = eseq.getExptype().getSeq();
		PODefinitionList podefs = getPattern().getDefinitions(stype.seqof);
		TCDefinitionList tcdefs = new TCDefinitionList();

		for (PODefinition podef: podefs)
		{
			if (podef instanceof POLocalDefinition)		// pattern defs will be local
			{
				tcdefs.add(new TCLocalDefinition(location, podef.name, podef.getType()));
			}
		}

		tcdefs.add(new TCLocalDefinition(location, ghostDef.name, ghostDef.type));
		Environment local = new FlatCheckedEnvironment(tcdefs, env, NameScope.NAMES);
		updates.addAll(getPattern().getVariableNames());
		updates.add(ghostDef.name);

		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(false);	// Don't exclude loop vars now
		}

		ctxt.push(new POForAllContext(updates, local));
		ctxt.push(new POImpliesContext(ghostIsPrefix(ghostDef, eseq), invariant));
		ctxt.push(new POLetDefContext(ghostUpdate(ghostDef)));

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
		 * and GHOST$ set to the original sequence value.
		 */
		updates.remove(ghostDef.name);
		updates.removeAll(getPattern().getVariableNames());

		if (!annotations.isEmpty())
		{
			invariant = annotations.combine(true);
		}

		ctxt.push(new POLetDefContext(ghostFinal(ghostDef, eseq)));		// let GHOST$ = set in
		ctxt.push(new POForAllContext(updates, env));					// forall <changed variables>
		ctxt.push(new POImpliesContext(invariant));						// invariant => ...
		ctxt.popInto(popto, altCtxt.add());

		// The two alternatives in one added.
		ctxt.push(altCtxt);

		return obligations;
	}

	/**
	 * Find the POPattern that defines the loop variable(s).
	 */
	private POPattern getPattern()
	{
		if (patternBind.pattern != null)
		{
			return patternBind.pattern;
		}
		else if (patternBind.bind instanceof POTypeBind)
		{
			POTypeBind tb = (POTypeBind)patternBind.bind;
			return tb.pattern;
		}
		else if (patternBind.bind instanceof POSetBind)
		{
			POSetBind sb = (POSetBind)patternBind.bind;
			return sb.pattern;
		}
		else // (patternBind.bind instanceof POSeqBind)
		{
			POSeqBind sb = (POSeqBind)patternBind.bind;
			return sb.pattern;
		}
	}

	/**
	 * Produce "ghost := ghost ^ [x]"
	 */
	private POAssignmentDefinition ghostUpdate(POAssignmentDefinition ghostDef)
	{
		TCType etype = ghostDef.type.getSeq().seqof;
		POLocalDefinition vardef = new POLocalDefinition(location, ghostDef.name, ghostDef.type);
		POExpressionList elist = new POExpressionList();
		elist.add(getPattern().getMatchingExpression());
		TCTypeList tlist = new TCTypeList();
		tlist.add(etype);

		POSeqConcatExpression cat = new POSeqConcatExpression(
			new POVariableExpression(ghostDef.name, vardef),
			new LexKeywordToken(Token.CONCATENATE, location),
			new POSeqEnumExpression(location, elist, tlist), ghostDef.type, ghostDef.type);

		return new POAssignmentDefinition(ghostDef.name, ghostDef.type, cat, ghostDef.type);
	}

	/**
	 * Produce "ghost := <sequence>"
	 */
	private POAssignmentDefinition ghostFinal(POAssignmentDefinition ghostDef, POExpression eseq)
	{
		return new POAssignmentDefinition(ghostDef.name, ghostDef.type, eseq, ghostDef.type);
	}

	/**
	 * Produce "GHOST$ = sequence(1, ..., len GHOST$)"
	 */
	private POExpression ghostIsPrefix(POAssignmentDefinition ghostDef, POExpression eseq)
	{
		POLocalDefinition vardef = new POLocalDefinition(location, ghostDef.name, ghostDef.type);
		
		return new POEqualsExpression(
			new POVariableExpression(ghostDef.name, vardef),
			new LexKeywordToken(Token.EQUALS, location),
			new POSubseqExpression(
				eseq,
				new POIntegerLiteralExpression(LexIntegerToken.ONE),
				new POLenExpression(location,
					new POVariableExpression(ghostDef.name, vardef)), ghostDef.type, ghostDef.type),
			ghostDef.type, ghostDef.type);
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForPatternBindStatement(this, arg);
	}
}
