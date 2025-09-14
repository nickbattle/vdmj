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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.OrderedObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;

abstract public class PONumericBinaryExpression extends POBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public PONumericBinaryExpression(POExpression left, LexToken op, POExpression right,
		TCType ltype, TCType rtype)
	{
		super(left, op, right, ltype, rtype);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = super.getProofObligations(ctxt, pogState, env);
		obligations.addAll(getNonNilObligations(ctxt));
		return obligations;
	}
	
	/**
	 * Generate non-nil obligations for optional types.
	 */
	private ProofObligationList getNonNilObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();
		
		if (ltype instanceof TCOptionalType)
		{
			TCOptionalType op = (TCOptionalType)ltype;
			obligations.addAll(SubTypeObligation.getAllPOs(left, op.type, ltype, ctxt));
		}
		
		if (rtype instanceof TCOptionalType)
		{
			TCOptionalType op = (TCOptionalType)rtype;
			obligations.addAll(SubTypeObligation.getAllPOs(right, op.type, rtype, ctxt));
		}

		return obligations;
	}
	
	/**
	 * Generate ordering obligations, as used by the comparison operators.
	 */
	protected ProofObligationList getOrderedObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = getCommonOrderedObligations(ctxt);
		obligations.addAll(left.getProofObligations(ctxt, pogState, env));
		obligations.addAll(right.getProofObligations(ctxt, pogState, env));
		return obligations;
	}
	
	private ProofObligationList getCommonOrderedObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = getNonNilObligations(ctxt);
		TCTypeSet lset = new TCTypeSet();
		TCTypeSet rset = new TCTypeSet();
		
		if (ltype.isUnion(location))
		{
			lset.addAll(ltype.getUnion().types);
		}
		else
		{
			lset.add(ltype);
		}
		
		if (rtype.isUnion(location))
		{
			rset.addAll(rtype.getUnion().types);
		}
		else
		{
			rset.add(rtype);
		}

		// For each LHS type, if there is a RHS type that is compatible, we potentially
		// remember the type. If there is a RHS type that is incompatible, we note that
		// a PO is actually needed.
		
		boolean poNeeded = false;
		TCTypeSet poTypes = new TCTypeSet();
		
		for (TCType lhs: lset)
		{
			if (lhs.isOrdered(location))
			{
    			for (TCType rhs: rset)
    			{
    				if (rhs.isOrdered(location) && TypeComparator.compatible(lhs, rhs))
    				{
						poTypes.add(TypeComparator.isSubType(lhs, rhs) ? rhs : lhs);	// Widest
					}
    				else
    				{
    					poNeeded = true;
    				}
    			}
			}
			else
			{
				poNeeded = true;
			}
		}
		
		if (poNeeded && !poTypes.isEmpty())
		{
			obligations.addAll(OrderedObligation.getAllPOs(left, right, poTypes, ctxt));
		}
		
		return obligations;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNumericBinaryExpression(this, arg);
	}
	
	@Override
	protected TCTypeQualifier getLeftQualifier()
	{
		return TCTypeQualifier.getNumericQualifier();
	}
	
	@Override
	protected TCTypeQualifier getRightQualifier()
	{
		return TCTypeQualifier.getNumericQualifier();
	}
}
