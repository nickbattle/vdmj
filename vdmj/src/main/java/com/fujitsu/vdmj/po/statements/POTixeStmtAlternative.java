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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.po.patterns.POPatternBind;
import com.fujitsu.vdmj.po.patterns.POSeqBind;
import com.fujitsu.vdmj.po.patterns.POSetBind;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqMemberObligation;
import com.fujitsu.vdmj.pog.SetMemberObligation;

public class POTixeStmtAlternative
{
	public final POPatternBind patternBind;
	public final POStatement statement;

	public POTixeStmtAlternative(POPatternBind patternBind, POStatement stmt)
	{
		this.patternBind = patternBind;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return patternBind + " |-> " + statement;
	}

	public ProofObligationList getProofObligations(POContextStack ctxt)
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
			list.addAll(bind.set.getProofObligations(ctxt));

			list.add(new SetMemberObligation(bind.pattern.getMatchingExpression(), bind.set, ctxt));
		}
		else if (patternBind.bind instanceof POSeqBind)
		{
			POSeqBind bind = (POSeqBind)patternBind.bind;
			list.addAll(bind.sequence.getProofObligations(ctxt));

			list.add(new SeqMemberObligation(bind.pattern.getMatchingExpression(), bind.sequence, ctxt));
		}


		list.addAll(statement.getProofObligations(ctxt));
		return list;
	}
}
