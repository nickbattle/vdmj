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
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.Utils;

public class POAtomicStatement extends POStatement
{
	private static final long serialVersionUID = 1L;

	public final POAssignmentStatementList assignments;

	public POAtomicStatement(LexLocation location, POAssignmentStatementList assignments)
	{
		super(location);
		this.assignments = assignments;
	}

	@Override
	public String toString()
	{
		return "atomic (" + Utils.listToString(assignments) + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		int var = 0;	// Create $atomic<++var> locals

		for (POAssignmentStatement stmt: assignments)
		{
			// This checks POs for the assignment expressions, creating $atomic<n> locals
			obligations.addAll(stmt.prepareAssignment(ctxt, pogState, env, ++var));
		}
		
		var = 0;

		for (POAssignmentStatement stmt: assignments)
		{
			// This assigns using the $atomic<n> locals created above
			obligations.addAll(stmt.completeAssignment(ctxt, pogState, env, ++var));
		}
		
		// Finally check any state invariant holds for the new assignments. This just uses
		// any of the assignments to find the stateDefinition.
		obligations.addAll(assignments.get(0).checkInvariant(ctxt));

		return obligations;
	}
	
	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAtomicStatement(this, arg);
	}
}
