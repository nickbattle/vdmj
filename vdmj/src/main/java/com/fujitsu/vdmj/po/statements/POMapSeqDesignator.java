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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SeqApplyObligation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
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
			list.addAll(SeqApplyObligation.getAllPOs(mapseq, exp, ctxt));
		}
		
		// Maps are OK, as you can create new map domain entries

		return list;
	}

	/**
	 * The simple updated variable name, x := 1, x(i) := 1 and x(i)(2).fld := 1
	 * all return the updated variable "x".
	 */
	@Override
	public TCNameToken updatedVariableName()
	{
		return mapseq.updatedVariableName();
	}

	/**
	 * The updated variable type, x := 1, x(i) := 1 and x(i)(2).fld := 1
	 * all return the type of the variable "x".
	 */
	@Override
	public TCType updatedVariableType()
	{
		return mapseq.updatedVariableType();
	}
	
	/**
	 * All variables used in a designator, eg. m(x).fld(y) is {m, x, y}
	 */
	@Override
	public TCNameSet getVariableNames()
	{
		TCNameSet set = mapseq.getVariableNames();
		set.addAll(exp.getVariableNames());
		return set;
	}
	
	/**
	 * All expressions used in a designator, eg. m(x).fld(y) is {m, x, y}
	 */
	@Override
	public POExpressionList getExpressions()
	{
		POExpressionList list = mapseq.getExpressions();
		list.add(exp);
		return list;
	}
}
