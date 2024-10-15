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
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;


abstract public class POSimpleBlockStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POStatementList statements;

	public POSimpleBlockStatement(LexLocation location, POStatementList statements)
	{
		super(location);
		this.statements = statements;
	}

	public void add(POStatement stmt)
	{
		statements.add(stmt);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";

		for (POStatement s: statements)
		{
			sb.append(sep);
			sb.append(s.toString());
			sep = ";\n";
		}

		sb.append("\n");
		return sb.toString();
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POContextStack globals, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		boolean hasUpdatedState = false;

		for (POStatement stmt: statements)
		{
			ProofObligationList oblist = stmt.getProofObligations(ctxt, globals, env);
			
			if (stmt.updatesState())
			{
				hasUpdatedState = true;
				oblist.markUnchecked(ProofObligation.BODY_UPDATES_STATE);
			}
			else if (stmt.readsState() && hasUpdatedState)
			{
				oblist.markUnchecked(ProofObligation.HAS_UPDATED_STATE);
			}

			obligations.addAll(oblist);
		}
		
		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSimpleBlockStatement(this, arg);
	}
}
