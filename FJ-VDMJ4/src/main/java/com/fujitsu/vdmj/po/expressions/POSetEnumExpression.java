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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.util.Utils;

public class POSetEnumExpression extends POSetExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpressionList members;
	public final TCTypeList types;

	public POSetEnumExpression(LexLocation location, POExpressionList members, TCTypeList types)
	{
		super(location);
		this.members = members;
		this.types = types;
	}

	@Override
	public String toString()
	{
		return Utils.listToString("{", members, ", ", "}");
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		return members.getProofObligations(ctxt);
	}
}
