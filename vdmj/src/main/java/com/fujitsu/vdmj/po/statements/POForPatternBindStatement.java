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
import com.fujitsu.vdmj.po.annotations.POLoopInvariantAnnotation;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POHeadExpression;
import com.fujitsu.vdmj.po.patterns.POIgnorePattern;
import com.fujitsu.vdmj.po.patterns.POPatternBind;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqMemberObligation;
import com.fujitsu.vdmj.pog.SetMemberObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class POForPatternBindStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POPatternBind patternBind;
	public final boolean reverse;
	public final POExpression sequence;
	public final TCType expType;
	public final POStatement statement;

	public POForPatternBindStatement(LexLocation location,
		POPatternBind patternBind, boolean reverse, POExpression exp, TCType expType, POStatement body)
	{
		super(location);
		this.patternBind = patternBind;
		this.reverse = reverse;
		this.sequence = exp;
		this.expType = expType;
		this.statement = body;
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
		ProofObligationList obligations = sequence.getProofObligations(ctxt, pogState, env);

		POLoopInvariantAnnotation annotation = annotations.getInstance(POLoopInvariantAnnotation.class);
		TCNameSet updates = statement.updatesState();
		
		if (annotation == null)		// No loop invariant defined
		{
			int popto = ctxt.size();
	
			if (patternBind.pattern != null)
			{
				ctxt.push(new POForAllSequenceContext(patternBind.pattern, sequence));
			}
			else if (patternBind.bind instanceof POTypeBind)
			{
				POTypeBind bind = (POTypeBind)patternBind.bind;
				ctxt.push(new POForAllSequenceContext(bind, sequence));
				
				TCSeqType s = expType.isSeq(location) ? expType.getSeq() : null;
				
				if (s != null && !TypeComparator.isSubType(s.seqof, bind.type))
				{
					obligations.addAll(SubTypeObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.type, s.seqof, ctxt));
				}
			}
			else if (patternBind.bind instanceof POSetBind)
			{
				POSetBind bind = (POSetBind)patternBind.bind;
				obligations.addAll(bind.set.getProofObligations(ctxt, pogState, env));
				
				ctxt.push(new POForAllSequenceContext(bind, sequence));
				obligations.addAll(SetMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.set, ctxt));
			}
			else if (patternBind.bind instanceof POSeqBind)
			{
				POSeqBind bind = (POSeqBind)patternBind.bind;
				obligations.addAll(bind.sequence.getProofObligations(ctxt, pogState, env));
				
				ctxt.push(new POForAllSequenceContext(bind, sequence));
				obligations.addAll(SeqMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.sequence, ctxt));
			}
	
			ProofObligationList loops = statement.getProofObligations(ctxt, pogState, env);
			ctxt.popTo(popto);
	
			if (statement.getStmttype().hasReturn())
			{
				updates.add(TCNameToken.getResult(location));
			}
			
			if (!updates.isEmpty())
			{
				ctxt.push(new POAmbiguousContext("for loop", updates, location));
			}
	
			obligations.addAll(loops);
			return obligations;
		}
		else
		{
			int popto = ctxt.size();
			POHeadExpression head = new POHeadExpression(location, sequence, expType);
			
			if (patternBind.pattern != null)
			{
				if (patternBind.pattern instanceof POIgnorePattern)
				{
					obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
					obligations.lastElement().setMessage("check initial for-loop");
				}
				else
				{
					POValueDefinition headdef = new POValueDefinition(annotations, patternBind.pattern, null, head, null, null);
					ctxt.push(new POLetDefContext(headdef));
					obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
					obligations.lastElement().setMessage("check initial for-loop");
					ctxt.pop();
				}

				ctxt.push(new POForAllSequenceContext(patternBind.pattern, sequence));
			}
			else if (patternBind.bind instanceof POTypeBind)
			{
				POTypeBind bind = (POTypeBind)patternBind.bind;

				POValueDefinition headdef = new POValueDefinition(annotations, bind.pattern, bind.type, head, null, null);
				ctxt.push(new POLetDefContext(headdef));
				obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
				obligations.lastElement().setMessage("check initial for-loop");
				ctxt.pop();

				ctxt.push(new POForAllSequenceContext(bind, sequence));
				
				TCSeqType s = expType.isSeq(location) ? expType.getSeq() : null;
				
				if (s != null && !TypeComparator.isSubType(s.seqof, bind.type))
				{
					obligations.addAll(SubTypeObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.type, s.seqof, ctxt));
				}
			}
			else if (patternBind.bind instanceof POSetBind)
			{
				POSetBind bind = (POSetBind)patternBind.bind;
				obligations.addAll(bind.set.getProofObligations(ctxt, pogState, env));

				POValueDefinition headdef = new POValueDefinition(annotations, bind.pattern, null, head, null, null);
				ctxt.push(new POLetDefContext(headdef));
				obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
				obligations.lastElement().setMessage("check initial for-loop");
				ctxt.pop();

				ctxt.push(new POForAllSequenceContext(bind, sequence));
				obligations.addAll(SetMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.set, ctxt));
			}
			else if (patternBind.bind instanceof POSeqBind)
			{
				POSeqBind bind = (POSeqBind)patternBind.bind;
				obligations.addAll(bind.sequence.getProofObligations(ctxt, pogState, env));

				POValueDefinition headdef = new POValueDefinition(annotations, bind.pattern, null, head, null, null);
				ctxt.push(new POLetDefContext(headdef));
				obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
				obligations.lastElement().setMessage("check initial for-loop");
				ctxt.pop();

				ctxt.push(new POForAllSequenceContext(bind, sequence));
				obligations.addAll(SeqMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.sequence, ctxt));
			}
	
			ctxt.push(new POImpliesContext(annotation.invariant));	// invariant => ...
			ProofObligationList loops = statement.getProofObligations(ctxt, pogState, env);
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check after for-loop");

			ctxt.popTo(popto);
	
			// Leave implication for following POs
			ctxt.push(new POImpliesContext(annotation.invariant));	// invariant => ...

			obligations.addAll(loops);
			return obligations;
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForPatternBindStatement(this, arg);
	}
}
