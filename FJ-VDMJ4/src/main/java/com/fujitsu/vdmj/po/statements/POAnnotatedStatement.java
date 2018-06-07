/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public class POAnnotatedStatement extends POStatement
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken name;
	
	public final POExpressionList args;

	public final POStatement statement;
	
	public POAnnotatedStatement(LexLocation location, TCIdentifierToken name, POExpressionList args, POStatement statement)
	{
		super(location);
		this.name = name;
		this.args = args;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return statement.toString();	// Note: exclude @Name
	}
	
	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return statement.getProofObligations(ctxt);
	}

	@Override
	public boolean hasSideEffects()
	{
		return statement.hasSideEffects();
	}
}
