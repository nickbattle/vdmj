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
import com.fujitsu.vdmj.scheduler.SystemClock;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.Value;

public class INTimeExpression extends INExpression
{
	private static final long serialVersionUID = 1L;

	public INTimeExpression(LexLocation location)
	{
		super(location);
	}

	@Override
	public Value eval(Context ctxt)
	{
		location.hit();
		
		assertNotInit(ctxt);

		try
        {
	        return new NaturalValue(SystemClock.getWallTime());
        }
        catch (Exception e)
        {
        	return abort(4145, "Time: " + e.getMessage(), ctxt);
        }
	}

	@Override
	public String toString()
	{
		return "time";
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTimeExpression(this, arg);
	}
}
