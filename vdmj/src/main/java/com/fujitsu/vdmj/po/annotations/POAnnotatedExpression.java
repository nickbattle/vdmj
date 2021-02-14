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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.annotations;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;

public class POAnnotatedExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	
	public final POAnnotation annotation;

	public final POExpression expression;
	
	public POAnnotatedExpression(LexLocation location, POAnnotation annotation, POExpression expression)
	{
		super(location);
		this.annotation = annotation;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "/* " + annotation + " */ " + expression;
	}
	
	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		annotation.poBefore(this, ctxt);
		ProofObligationList obligations = expression.getProofObligations(ctxt);
		annotation.poAfter(this, obligations, ctxt);
		return obligations;
	}

	@Override
	public String getPreName()
	{
		return expression.getPreName();
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAnnotatedExpression(this, arg);
	}
}
