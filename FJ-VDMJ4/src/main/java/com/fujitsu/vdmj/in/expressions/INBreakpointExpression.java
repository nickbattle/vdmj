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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.BreakpointCondition;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;

public class INBreakpointExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	private final Breakpoint bp;
	private final BreakpointCondition cond;
	private final long arg;

	public INBreakpointExpression(
		Breakpoint breakpoint, BreakpointCondition cond, long arg)
	{
		super(breakpoint.location);
		this.bp = breakpoint;
		this.cond = cond;
		this.arg = arg;
	}

	@Override
	public String toString()
	{
		return "hits " + cond + " " + arg;
	}

	@Override
	public Value eval(Context ctxt)
	{
		boolean rv = false;

		switch (cond)
		{
			case EQ:
				rv = (bp.hits == arg);
				break;

			case GT:
				rv = (bp.hits > arg);
				break;

			case GE:
				rv = (bp.hits >= arg);
				break;

			case MOD:
				rv = ((bp.hits % arg) == 0);
				break;
		}

		return new BooleanValue(rv);
	}
}
