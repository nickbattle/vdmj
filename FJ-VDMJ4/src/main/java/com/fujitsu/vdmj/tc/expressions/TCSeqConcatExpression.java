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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSeqConcatExpression extends TCBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public TCSeqConcatExpression(TCExpression left, LexToken op, TCExpression right)
	{
		super(left, op, right);
	}

	@Override
	public final TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		if (constraint != null && constraint.isSeq(location))
		{
			TCSeqType c = constraint.getSeq();
			
			if (c instanceof TCSeq1Type)	// constraint of LHS/RHS are not seq1
			{
				constraint = new TCSeqType(c.location, c.seqof);
			}
		}

		ltype = left.typeCheck(env, null, scope, constraint);
		rtype = right.typeCheck(env, null, scope, constraint);

		if (!ltype.isSeq(location))
		{
			report(3157, "Left hand of '^' is not a sequence");
			ltype = new TCSeqType(location, new TCUnknownType(location));
		}

		if (!rtype.isSeq(location))
		{
			report(3158, "Right hand of '^' is not a sequence");
			rtype = new TCSeqType(location, new TCUnknownType(location));
		}

		TCType lof = ltype.getSeq();
		TCType rof = rtype.getSeq();
		boolean seq1 = (lof instanceof TCSeq1Type) || (rof instanceof TCSeq1Type);
		
		lof = ((TCSeqType)lof).seqof;
		rof = ((TCSeqType)rof).seqof;
		TCTypeSet ts = new TCTypeSet(lof, rof);
		
		return seq1 ?
			new TCSeq1Type(location, ts.getType(location)) :
			new TCSeqType(location, ts.getType(location));
	}
}
