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

import java.util.List;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;

public class INPowerSetExpression extends INUnaryExpression
{
	private static final long serialVersionUID = 1L;

	public INPowerSetExpression(LexLocation location, INExpression exp)
	{
		super(location, exp);
	}

	@Override
	public String toString()
	{
		return "(power " + exp + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		ValueSet values = exp.eval(ctxt).setValue(ctxt);
    		List<ValueSet> psets = values.powerSet(breakpoint, ctxt);
			ValueSet rs = new ValueSet(psets.size());

    		for (ValueSet v: psets)
    		{
    			rs.addNoCheck(new SetValue(v));
    		}

    		// The additions above can take a while, because all of the SetValues are
    		// sorted. So we check the interrupt flag afterwards to try to respond.
    		
			if (Breakpoint.execInterruptLevel() > 0)
			{
				breakpoint.enterDebugger(ctxt);
			}

			Value ps = new SetValue(rs);
			
			// And again here, the sort above can take a while, so we re-check the
			// interrupt flag to try to respond while within the power expression.
			
			if (Breakpoint.execInterruptLevel() > 0)
			{
				breakpoint.enterDebugger(ctxt);
			}

			return ps;
		}
		catch (ValueException e)
		{
			return abort(e);
		}
		catch (InternalException e)		// From powerSet
		{
			throw new ContextException(e.number, e.getMessage(), location, ctxt);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePowerSetExpression(this, arg);
	}
}
