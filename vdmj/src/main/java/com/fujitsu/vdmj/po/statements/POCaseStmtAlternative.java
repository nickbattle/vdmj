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
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.pog.POCaseContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.PONotCaseContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;

public class POCaseStmtAlternative extends PONode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final POExpression cexp;
	public final POPattern pattern;
	public final POStatement statement;

	public POCaseStmtAlternative(POExpression cexp, POPattern pattern, POStatement stmt)
	{
		this.location = pattern.location;
		this.cexp = cexp;
		this.pattern = pattern;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "case " + pattern + " -> " + statement;
	}

	public ProofObligationList getProofObligations(POContextStack ctxt, POContextStack globals, TCType type, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();

		ctxt.push(new POCaseContext(pattern, type, cexp));
		obligations.addAll(statement.getProofObligations(ctxt, globals, env));
		ctxt.pop();
		ctxt.push(new PONotCaseContext(pattern, type, cexp));

		return obligations;
	}
}
