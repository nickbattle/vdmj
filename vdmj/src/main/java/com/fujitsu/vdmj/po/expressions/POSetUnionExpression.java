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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeQualifier;

public class POSetUnionExpression extends POBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public POSetUnionExpression(POExpression left, LexToken op, POExpression right,
		TCType ltype, TCType rtype)
	{
		super(left, op, right, ltype, rtype);
	}

	@Override
	public String toString()
	{
		return left + " union " + right;
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetUnionExpression(this, arg);
	}

	@Override
	protected TCTypeQualifier getLeftQualifier()
	{
		return TCTypeQualifier.getSetQualifier();
	}

	@Override
	protected TCTypeQualifier getRightQualifier()
	{
		return TCTypeQualifier.getSetQualifier();
	}
}
