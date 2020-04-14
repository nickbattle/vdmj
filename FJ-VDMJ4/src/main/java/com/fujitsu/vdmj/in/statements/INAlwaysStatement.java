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

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExitException;
import com.fujitsu.vdmj.values.Value;

public class INAlwaysStatement extends INStatement
{
	private static final long serialVersionUID = 1L;

	public final INStatement always;
	public final INStatement body;

	public INAlwaysStatement(LexLocation location, INStatement always, INStatement body)
	{
		super(location);
		this.always = always;
		this.body = body;
	}

	@Override
	public String toString()
	{
		return "always " + always + " in " + body;
	}

	@Override
	public INStatement findStatement(int lineno)
	{
		INStatement found = super.findStatement(lineno);
		if (found != null) return found;
		found = always.findStatement(lineno);
		if (found != null) return found;
		return body.findStatement(lineno);
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = always.findExpression(lineno);
		if (found != null) return found;
		return body.findExpression(lineno);
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value rv = null;
		ExitException bodyRaised = null;

		try
		{
			rv = body.eval(ctxt);
		}
		catch (ExitException e)
		{
			// Finally clause executes the "always" statement, but we
			// re-throw this exception, unless the always clause raises one.

			bodyRaised = e;
		}
		finally
		{
			always.eval(ctxt);

			if (bodyRaised != null)
			{
				throw bodyRaised;
			}
		}

		return rv;
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAlwaysStatement(this, arg);
	}
}
