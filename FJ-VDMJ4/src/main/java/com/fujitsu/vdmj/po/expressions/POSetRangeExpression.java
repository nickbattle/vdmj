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
import com.fujitsu.vdmj.tc.types.TCType;

public class POSetRangeExpression extends POSetExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression first;
	public final POExpression last;
	public final TCType ftype;
	public final TCType ltype;

	public POSetRangeExpression(LexLocation start, POExpression first, POExpression last,
		TCType ftype, TCType ltype)
	{
		super(start);
		this.first = first;
		this.last = last;
		this.ftype = ftype;
		this.ltype = ltype;
	}

	@Override
	public String toString()
	{
		return "{" + first + ", ... ," + last + "}";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = first.getProofObligations(ctxt);
		obligations.addAll(last.getProofObligations(ctxt));
		return obligations;
	}
}
