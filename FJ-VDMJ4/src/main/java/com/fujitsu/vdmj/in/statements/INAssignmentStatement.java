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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

public class INAssignmentStatement extends INStatement
{
	private static final long serialVersionUID = 1L;

	public final INExpression exp;
	public final INStateDesignator target;
	public final TCType targetType;

	public INAssignmentStatement(LexLocation location, INStateDesignator target, INExpression exp, TCType targetType)
	{
		super(location);
		this.exp = exp;
		this.target = target;
		this.targetType = targetType;
	}

	@Override
	public String toString()
	{
		return target + " := " + exp;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value newval = exp.eval(ctxt);
		Value oldval = target.eval(ctxt);

		try
		{
			oldval.set(location, newval.convertTo(targetType, ctxt), ctxt);
		}
		catch (ValueException e)
		{
			abort(e);
		}

		if (Settings.dialect == Dialect.VDM_RT &&
			Properties.rt_log_instvarchanges)
		{
			ObjectValue self = ctxt.getSelf();	// May be a static

			// The showtrace plugin does not like "quotes", nor does it
			// have a \" type convention, so we substitute for apostrophes.
			String noquotes = newval.toString().replaceAll("\\\"", "\'");

			if (self == null)
			{
    			RTLogger.log(
    				"InstVarChange -> instnm: \"" + target.toString() + "\"" +
    				" val: \"" + noquotes + "\"" +
    				" objref: nil" +
    				" id: " + Thread.currentThread().getId());
			}
			else
			{
    			RTLogger.log(
    				"InstVarChange -> instnm: \"" + target.toString() + "\"" +
    				" val: \"" + noquotes + "\"" +
    				" objref: " + self.objectReference +
    				" id: " + Thread.currentThread().getId());
			}
		}

		return new VoidValue();
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAssignmentStatement(this, arg);
	}
}
