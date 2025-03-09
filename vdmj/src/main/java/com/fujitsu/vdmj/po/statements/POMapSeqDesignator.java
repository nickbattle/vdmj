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

import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqApplyObligation;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;

public class POMapSeqDesignator extends POStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final POStateDesignator mapseq;
	public final POExpression exp;
	public final TCSeqType seqType;

	public POMapSeqDesignator(POStateDesignator mapseq, POExpression exp, TCSeqType seqType)
	{
		super(mapseq.location);
		this.mapseq = mapseq;
		this.exp = exp;
		this.seqType = seqType;
	}

	@Override
	public String toString()
	{
		return mapseq + "(" + exp + ")";
	}
	
	@Override
	public POExpression toExpression()
	{
		POExpression root = mapseq.toExpression();
		POExpressionList args = new POExpressionList();
		args.add(exp);
		TCTypeList argtypes = new TCTypeList();
		TCType type = new TCUnknownType(location);
		argtypes.add(type);
		return new POApplyExpression(root, args , type, argtypes, null, null);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList list = mapseq.getProofObligations(ctxt);

		if (seqType != null)
		{
			list.add(new SeqApplyObligation(mapseq, exp, ctxt));
		}
		
		// Maps are OK, as you can create new map domain entries

		return list;
	}
}
