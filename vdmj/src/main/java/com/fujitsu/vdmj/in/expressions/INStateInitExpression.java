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

import com.fujitsu.vdmj.in.definitions.INStateDefinition;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;

public class INStateInitExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INStateDefinition state;

	public INStateInitExpression(LexLocation location, INStateDefinition state)
	{
		super(location);
		this.state = state;
		location.executable(false);
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			FunctionValue invariant = state.invfunc;

			// Note, the function just checks whether the argument passed would
			// violate the state invariant (if any). It doesn't initialize the
			// state itself. This is done in State.initialize().

			if (invariant != null)
			{
				INIdentifierPattern argp = (INIdentifierPattern)state.initPattern;
				RecordValue rv = (RecordValue)ctxt.lookup(argp.name);
				return invariant.eval(location, rv, ctxt);
			}

			return new BooleanValue(true);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public String toString()
	{
		return "init " + state.initPattern + " == " + state.initExpression;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseStateInitExpression(this, arg);
	}
}
