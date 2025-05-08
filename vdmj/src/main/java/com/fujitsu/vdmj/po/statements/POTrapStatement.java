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
import com.fujitsu.vdmj.po.patterns.POPatternBind;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqMemberObligation;
import com.fujitsu.vdmj.pog.SetMemberObligation;
import com.fujitsu.vdmj.typechecker.Environment;

public class POTrapStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POPatternBind patternBind;
	public final POStatement with;
	public final POStatement body;

	public POTrapStatement(LexLocation location,
		POPatternBind patternBind, POStatement with, POStatement body)
	{
		super(location);
		this.patternBind = patternBind;
		this.with = with;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "trap " + patternBind + " with " + with + " in " + body;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList list = new ProofObligationList();

		if (patternBind.pattern != null)
		{
			// Nothing to do
		}
		else if (patternBind.bind instanceof POTypeBind)
		{
			// Nothing to do
		}
		else if (patternBind.bind instanceof POSetBind)
		{
			POSetBind bind = (POSetBind)patternBind.bind;
			list.addAll(bind.set.getProofObligations(ctxt, pogState, env));

			list.addAll(SetMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.set, ctxt));
		}
		else if (patternBind.bind instanceof POSeqBind)
		{
			POSeqBind bind = (POSeqBind)patternBind.bind;
			list.addAll(bind.sequence.getProofObligations(ctxt, pogState, env));

			list.addAll(SeqMemberObligation.getAllPOs(bind.pattern.getMatchingExpression(), bind.sequence, ctxt));
		}
		
		// The "with" clause sees the "body" state updates, so this comes first
		list.addAll(body.getProofObligations(ctxt, pogState, env));
		
		// We don't know the exception type to match against the trap pattern/bind, so unchecked
		int popto = ctxt.size();
		list.addAll(with.getProofObligations(ctxt, pogState, env).markUnchecked(ProofObligation.NOT_YET_SUPPORTED));
		ctxt.popTo(popto);
		
		ctxt.push(new POAmbiguousContext("trap statement", ctxt.getStateVariables(), location));

		return list;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTrapStatement(this, arg);
	}
}
