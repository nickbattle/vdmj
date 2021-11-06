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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POTypeBind;
import com.fujitsu.vdmj.po.patterns.POTypeBindList;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.types.TCFunctionType;

public class POLambdaExpression extends POExpression
{
	private static final long serialVersionUID = 1L;
	public final POTypeBindList bindList;
	public final POExpression expression;
	public final TCFunctionType type;
	public final POPatternList paramPatterns;
	public final PODefinitionList paramDefinitions;

	public POLambdaExpression(LexLocation location,
			POTypeBindList bindList, POExpression expression,
			TCFunctionType type,
			POPatternList paramPatterns,
			PODefinitionList paramDefinitions)
	{
		super(location);
		this.bindList = bindList;
		this.expression = expression;
		this.type = type;
		this.paramDefinitions = paramDefinitions;
		this.paramPatterns = paramPatterns;
	}

	@Override
	public String toString()
	{
		return "(lambda " + bindList + " & " + expression + ")";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
	{
		ProofObligationList obligations = new ProofObligationList();

		for (POTypeBind tb: bindList)
		{
			obligations.addAll(tb.getProofObligations(ctxt));
		}

		ctxt.push(new POForAllContext(this));
		obligations.addAll(expression.getProofObligations(ctxt));
		ctxt.pop();

		return obligations;
	}
	
	@Override
	public String getPreName()
	{
		return "";	// lambdas are functions without preconditions
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLambdaExpression(this, arg);
	}
}
