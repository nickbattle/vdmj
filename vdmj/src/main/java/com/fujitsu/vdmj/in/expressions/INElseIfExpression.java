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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;

public class INElseIfExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression elseIfExp;
	public final INExpression thenExp;

	public INElseIfExpression(LexLocation location,
			INExpression elseIfExp, INExpression thenExp)
	{
		super(location);
		this.elseIfExp = elseIfExp;
		this.thenExp = thenExp;
	}

	@Override
	public String toString()
	{
		return "elseif " + elseIfExp + "\nthen " + thenExp;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			return elseIfExp.eval(ctxt).boolValue(ctxt) ? thenExp.eval(ctxt) : null;
		}
        catch (ValueException e)
        {
        	return abort(e);
        }
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseElseIfExpression(this, arg);
	}
}
