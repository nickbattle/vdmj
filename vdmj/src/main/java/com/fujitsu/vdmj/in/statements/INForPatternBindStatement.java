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

package com.fujitsu.vdmj.in.statements;

import java.util.ListIterator;

import com.fujitsu.vdmj.in.annotations.INLoopAnnotations;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INPatternBind;
import com.fujitsu.vdmj.in.patterns.INSeqBind;
import com.fujitsu.vdmj.in.patterns.INSetBind;
import com.fujitsu.vdmj.in.patterns.INTypeBind;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;
import com.fujitsu.vdmj.values.VoidValue;

public class INForPatternBindStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INPatternBind patternBind;
	public final boolean reverse;
	public final INExpression seqexp;
	public final INStatement statement;
	public final INLoopAnnotations invariants;

	public INForPatternBindStatement(LexLocation location,
		INPatternBind patternBind, boolean reverse, INExpression seqexp,
		INStatement body, INLoopAnnotations invariants)
	{
		super(location);
		this.patternBind = patternBind;
		this.reverse = reverse;
		this.seqexp = seqexp;
		this.statement = body;
		this.invariants = invariants;
	}

	@Override
	public String toString()
	{
		return "for " + patternBind + " in " +
			(reverse ? " reverse " : "") + seqexp + " do\n" + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			ValueList values = seqexp.eval(ctxt).seqValue(ctxt);

			if (reverse)
			{
				ListIterator<Value> li = values.listIterator(values.size());
				ValueList backwards = new ValueList();

				while (li.hasPrevious())
				{
					backwards.add(li.previous());
				}

				values = backwards;
			}

			invariants.before(ctxt);
			invariants.check(ctxt);

			if (patternBind.pattern != null)
			{
				for (Value val: values)
				{
					try
					{
						Context evalContext = new Context(location, "for pattern", ctxt);
						evalContext.putList(patternBind.pattern.getNamedValues(val, ctxt));

						invariants.check(ctxt);
						Value rv = statement.eval(evalContext);
						invariants.check(ctxt);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
				}
			}
			else if (patternBind.bind instanceof INSetBind)
			{
				INSetBind setbind = (INSetBind)patternBind.bind;
				ValueSet set = setbind.set.eval(ctxt).setValue(ctxt);

				for (Value val: values)
				{
					try
					{
						if (!set.contains(val))
						{
							abort(4039, "Set bind does not contain value " + val, ctxt);
						}

						Context evalContext = new Context(location, "for set bind", ctxt);
						evalContext.putList(setbind.pattern.getNamedValues(val, ctxt));

						invariants.check(ctxt);
						Value rv = statement.eval(evalContext);
						invariants.check(ctxt);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
				}
			}
			else if (patternBind.bind instanceof INSeqBind)
			{
				INSeqBind seqbind = (INSeqBind)patternBind.bind;
				ValueList seq = seqbind.sequence.eval(ctxt).seqValue(ctxt);

				for (Value val: values)
				{
					try
					{
						if (!seq.contains(val))
						{
							abort(4039, "Seq bind does not contain value " + val, ctxt);
						}

						Context evalContext = new Context(location, "for seq bind", ctxt);
						evalContext.putList(seqbind.pattern.getNamedValues(val, ctxt));

						invariants.check(ctxt);
						Value rv = statement.eval(evalContext);
						invariants.check(ctxt);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
				}
			}
			else
			{
				INTypeBind typebind = (INTypeBind)patternBind.bind;

				for (Value val: values)
				{
					try
					{
						Value converted = val.convertTo(typebind.type, ctxt);

						Context evalContext = new Context(location, "for type bind", ctxt);
						evalContext.putList(typebind.pattern.getNamedValues(converted, ctxt));

						invariants.check(ctxt);
						Value rv = statement.eval(evalContext);
						invariants.check(ctxt);

						if (!rv.isVoid())
						{
							return rv;
						}
					}
					catch (PatternMatchException e)
					{
						// Ignore mismatches
					}
				}
			}

			// Clean up any ghost variables
			invariants.after(ctxt);
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
		return visitor.caseForPatternBindStatement(this, arg);
	}
}
