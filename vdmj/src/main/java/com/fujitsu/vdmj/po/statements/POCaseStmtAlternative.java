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
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POCaseStmtAlternative extends PONode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final POPattern pattern;
	public final POStatement statement;

	public POCaseStmtAlternative(POPattern pattern, POStatement stmt)
	{
		this.location = pattern.location;
		this.pattern = pattern;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "case " + pattern + " -> " + statement;
	}

	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		obligations.addAll(statement.getProofObligations(ctxt, env));
		return obligations;
	}
}
