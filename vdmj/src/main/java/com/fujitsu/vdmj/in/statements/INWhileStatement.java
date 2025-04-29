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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.annotations.INLoopInvariantAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

public class INWhileStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression exp;
	public final INStatement statement;

	public INWhileStatement(LexLocation location, INExpression exp, INStatement body)
	{
		super(location);
		this.exp = exp;
		this.statement = body;
	}

	@Override
	public String toString()
	{
		return "while " + exp + " do " + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		
		INLoopInvariantAnnotation invariant = annotations.getInstance(INLoopInvariantAnnotation.class);

		try
		{
			if (invariant == null)
			{
				while (exp.eval(ctxt).boolValue(ctxt))
				{
					Value rv = statement.eval(ctxt);
	
					if (!rv.isVoid())
					{
						return rv;
					}
				}
			}
			else
			{
				invariant.check(ctxt);
				
				while (exp.eval(ctxt).boolValue(ctxt))
				{
					invariant.check(ctxt);
					Value rv = statement.eval(ctxt);
	
					if (!rv.isVoid())
					{
						return rv;
					}

					invariant.check(ctxt);
				}
			}
		}
		catch (ValueException e)
		{
			abort(e);
		}

		return new VoidValue();
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseWhileStatement(this, arg);
	}
}
