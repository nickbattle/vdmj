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
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POGStateList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POTixeStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POTixeStmtAlternativeList traps;
	public final POStatement body;

	public POTixeStatement(LexLocation location, POTixeStmtAlternativeList traps, POStatement body)
	{
		super(location);
		this.traps = traps;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "tixe {" + traps + "} in " + body;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		POGStateList stateList = new POGStateList();

		// The trap clauses see the "body" state updates, so this comes first
		obligations.addAll(body.getProofObligations(ctxt, pogState, env));

		for (POTixeStmtAlternative alt: traps)
		{
			int popto = ctxt.size();
			obligations.addAll(alt.getProofObligations(ctxt, stateList.addCopy(pogState), env));
			ctxt.popTo(popto);
		}

		stateList.combineInto(pogState);
		ctxt.push(new POAmbiguousContext("tixe statement", pogState, location));
		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTixeStatement(this, arg);
	}
}
