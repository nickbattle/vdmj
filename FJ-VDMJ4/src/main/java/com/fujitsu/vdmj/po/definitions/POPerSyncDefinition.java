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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.PONameContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;

public class POPerSyncDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final POExpression guard;

	public POPerSyncDefinition(LexLocation location, TCNameToken opname, POExpression guard)
	{
		super(location, opname.getPerName(location));
		this.opname = opname;
		this.guard = guard;
	}

	@Override
	public TCType getType()
	{
		return new TCBooleanType(location);
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList();
	}

	@Override
	public String toString()
	{
		return "per " + opname + " => " + guard;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ctxt.push(new PONameContext(new TCNameList(opname)));
		ProofObligationList list = guard.getProofObligations(ctxt);
		ctxt.pop();
		return list;
	}
}
