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
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPatternBind;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqMemberObligation;
import com.fujitsu.vdmj.pog.SetMemberObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
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
		ProofObligationList list = sequence.getProofObligations(ctxt, pogState, env);

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
				list.addAll(SubTypeObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.type, s.seqof, ctxt));
			}
		}
		else if (patternBind.bind instanceof POSetBind)
		{
			POSetBind bind = (POSetBind)patternBind.bind;
			list.addAll(bind.set.getProofObligations(ctxt, pogState, env));
			
			ctxt.push(new POForAllSequenceContext(bind, sequence));
			list.addAll(SetMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.set, ctxt));
		}
		else if (patternBind.bind instanceof POSeqBind)
		{
			POSeqBind bind = (POSeqBind)patternBind.bind;
			list.addAll(bind.sequence.getProofObligations(ctxt, pogState, env));
			
			ctxt.push(new POForAllSequenceContext(bind, sequence));
			list.addAll(SeqMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.sequence, ctxt));
		}

		POGState copy = pogState.getCopy();
		ProofObligationList loops = statement.getProofObligations(ctxt, copy, env);
		pogState.combineWith(copy);
		ctxt.popTo(popto);

		TCNameSet updates = statement.updatesState();

		if (!updates.isEmpty())
		{
			ctxt.push(new POAmbiguousContext("for loop", updates, location));
		}

		list.addAll(loops);
		return list;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForPatternBindStatement(this, arg);
	}
}
