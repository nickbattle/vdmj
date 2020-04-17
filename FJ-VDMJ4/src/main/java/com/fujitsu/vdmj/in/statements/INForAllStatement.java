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
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;
import com.fujitsu.vdmj.values.VoidValue;

public class INForAllStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INPattern pattern;
	public final INExpression set;
	public final INStatement statement;

	public INForAllStatement(LexLocation location,
		INPattern pattern, INExpression set, INStatement stmt)
	{
		super(location);
		this.pattern = pattern;
		this.set = set;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "for all " + pattern + " in set " + set + " do\n" + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			ValueSet values = set.eval(ctxt).setValue(ctxt);

			for (Value val: values)
			{
				try
				{
					Context evalContext = new Context(location, "for all", ctxt);
					evalContext.putList(pattern.getNamedValues(val, ctxt));
					Value rv = statement.eval(evalContext);

					if (!rv.isVoid())
					{
						return rv;
					}
				}
				catch (PatternMatchException e)
				{
					// Ignore and try others
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
		return visitor.caseForAllStatement(this, arg);
	}
}
