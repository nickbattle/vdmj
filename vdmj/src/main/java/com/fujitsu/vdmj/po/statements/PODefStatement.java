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
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.Utils;

public class PODefStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final PODefinitionList equalsDefs;
	public final POStatement statement;

	public PODefStatement(LexLocation location, PODefinitionList equalsDefs, POStatement statement)
	{
		super(location);
		this.equalsDefs = equalsDefs;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return "def " + Utils.listToString(equalsDefs) + " in " + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = equalsDefs.getDefProofObligations(ctxt, pogState, env);

		int popto = ctxt.pushAt(new POLetDefContext(equalsDefs));
		obligations.addAll(statement.getProofObligations(ctxt, pogState, env));
		
		if (ctxt.size() == popto + 1)
		{
			// Nothing left on the stack by the body, so remove this context
			ctxt.popTo(popto);
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDefStatement(this, arg);
	}
}
