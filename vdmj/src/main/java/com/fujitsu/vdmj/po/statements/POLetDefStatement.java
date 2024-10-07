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
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;

public class POLetDefStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final PODefinitionList localDefs;
	public final POStatement statement;

	public POLetDefStatement(LexLocation location, PODefinitionList localDefs, POStatement statement)
	{
		super(location);
		this.localDefs = localDefs;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return "let " + localDefs + " in " + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations = localDefs.getProofObligations(ctxt, env);

		ctxt.push(new POLetDefContext(this.localDefs));
		obligations.addAll(statement.getProofObligations(ctxt, env));
		ctxt.pop();

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLetDefStatement(this, arg);
	}
}
