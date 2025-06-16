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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.Value;

public class INThreadIdExpression extends INExpression
{
	private static final long serialVersionUID = 1L;

	public INThreadIdExpression(LexLocation location)
	{
		super(location);
	}

	@Override
	public Value eval(Context ctxt)
	{
		try
		{
			location.hit();
			return new NaturalValue(ctxt.threadState.threadId);
		}
		catch (Exception e)
		{
			return abort(4065, e.getMessage(), ctxt);
		}
	}

	@Override
	public String toString()
	{
		return "threadid";
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseThreadIdExpression(this, arg);
	}
}
