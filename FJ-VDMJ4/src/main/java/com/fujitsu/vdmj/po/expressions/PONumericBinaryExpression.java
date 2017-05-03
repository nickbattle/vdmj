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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.pog.OrderedObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;

abstract public class PONumericBinaryExpression extends POBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public PONumericBinaryExpression(POExpression left, LexToken op, POExpression right,
		TCType ltype, TCType rtype)
	{
		super(left, op, right, ltype, rtype);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();

		if (ltype.isUnion(location))
		{
			obligations.add(
				new SubTypeObligation(left, new TCRealType(left.location), ltype, ctxt));
		}

		if (rtype.isUnion(location))
		{
			obligations.add(
				new SubTypeObligation(right, new TCRealType(right.location), rtype, ctxt));
		}

		obligations.addAll(left.getProofObligations(ctxt));
		obligations.addAll(right.getProofObligations(ctxt));
		return obligations;
	}
	
	/**
	 * Generate ordering obligations, as used by the comparison operators.
	 */
	protected ProofObligationList getOrderedObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = getOrderedObligations(left, ltype, ctxt);
		obligations.addAll(getOrderedObligations(right, rtype, ctxt));
		obligations.addAll(left.getProofObligations(ctxt));
		obligations.addAll(right.getProofObligations(ctxt));
		return obligations;
	}
	
	private ProofObligationList getOrderedObligations(POExpression exp, TCType type, POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();
		
		if (type.isUnion(location))
		{
			// If any members are not ordered, then the left argument must not be that type
			TCTypeSet members = type.getUnion().types;
			TCTypeSet unordered = new TCTypeSet();
			
			for (TCType m: members)
			{
				if (!m.isOrdered(location))
				{
					unordered.add(m);
				}
			}
			
			if (!unordered.isEmpty())
			{
				obligations.add(new OrderedObligation(exp, unordered, ctxt));
			}
		}
		
		return obligations;
	}
}
