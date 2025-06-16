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

import java.util.Collections;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBind;
import com.fujitsu.vdmj.in.patterns.INSetBind;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;
import com.fujitsu.vdmj.values.ValueSet;

public class INSeqCompExpression extends INSeqExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression first;
	public final INBind bind;
	public final INExpression predicate;

	public INSeqCompExpression(LexLocation start,
		INExpression first, INBind bind, INExpression predicate)
	{
		super(start);
		this.first = first;
		this.bind = bind;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "[" + first + " | " + bind +
			(predicate == null ? "]" : " & " + predicate + "]");
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		ValueList allValues = null;
		
		try
		{
			allValues = bind.getBindValues(ctxt, false);
		}
		catch (ValueException e)
		{
			abort(e);
		}
		
		if (bind instanceof INSetBind)
		{
			return evalSetBind(allValues, ctxt);
		}
		else
		{
			return evalSeqBind(allValues, ctxt);
		}
	}
	
	private Value evalSetBind(ValueList allValues, Context ctxt)
	{
		ValueSet seq = new ValueSet();	// INBind variable values
		ValueMap map = new ValueMap();	// Map bind values to output values
		int count = 0;

		for (Value val: allValues)
		{
			try
			{
				Context evalContext = new Context(location, "seq comprehension", ctxt);
				NameValuePairList nvpl = bind.pattern.getNamedValues(val, ctxt);
				Value sortOn = nvpl.isEmpty() ? new NaturalValue(count++) : nvpl.get(0).value;

				if (map.get(sortOn) == null)
				{
    				if (nvpl.size() > 1 || !sortOn.isOrdered())
    				{
    					abort(4029, "Sequence comprehension binding must be one ordered value", ctxt);
    				}

    				evalContext.putList(nvpl);

    				try
					{
						if (predicate == null || predicate.eval(evalContext).boolValue(ctxt))
						{
							Value out = first.eval(evalContext);
							seq.add(sortOn);
							map.put(sortOn, out);
						}
					}
					catch (ValueException e)
					{
						predicate.abort(e);
					}
				}
			}
			catch (PatternMatchException e)
			{
				// Ignore mismatches
			}
			catch (ContextException e)
			{
				throw e;
			}
			catch (Exception e)
			{
				// Ignore NaturalValue exception
			}
		}

		Collections.sort(seq);	// Using compareTo
		ValueList sorted = new ValueList();

		for (Value bv: seq)
		{
			sorted.add(map.get(bv));
		}

		return new SeqValue(sorted);
	}

	private Value evalSeqBind(ValueList allValues, Context ctxt)
	{
		ValueList seq = new ValueList();	// INBind variable values

		for (Value val: allValues)
		{
			try
			{
				Context evalContext = new Context(location, "seq comprehension", ctxt);
				NameValuePairList nvpl = bind.pattern.getNamedValues(val, ctxt);

				evalContext.putList(nvpl);

				if (predicate == null || predicate.eval(evalContext).boolValue(ctxt))
				{
					seq.add(first.eval(evalContext));
				}
			}
			catch (ValueException e)
			{
				abort(e);
			}
			catch (PatternMatchException e)
			{
				// Ignore mismatches
			}
		}

		return new SeqValue(seq);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSeqCompExpression(this, arg);
	}
}
