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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;
import com.fujitsu.vdmj.typechecker.Environment;

public class POSubseqExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POExpression seq;
	public final POExpression from;
	public final POExpression to;
	public final TCType ftype;
	public final TCType ttype;

	public POSubseqExpression(POExpression seq, POExpression from, POExpression to,
		TCType ftype, TCType ttype)
	{
		super(seq);
		this.seq = seq;
		this.from = from;
		this.to = to;
		this.ftype = ftype;
		this.ttype = ttype;
	}

	@Override
	public String toString()
	{
		return "(" + seq + "(" + from + ", ... ," + to + "))";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList list = seq.getProofObligations(ctxt, env);
		list.addAll(from.getProofObligations(ctxt, env));
		list.addAll(to.getProofObligations(ctxt, env));
		list.addAll(checkUnionQualifiers(from, TCTypeQualifier.getNumericQualifier(), ctxt));
		list.addAll(checkUnionQualifiers(to, TCTypeQualifier.getNumericQualifier(), ctxt));
		return list;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSubseqExpression(this, arg);
	}
}
