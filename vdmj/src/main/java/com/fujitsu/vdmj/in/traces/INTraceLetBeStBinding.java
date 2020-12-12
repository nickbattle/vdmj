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

package com.fujitsu.vdmj.in.traces;

import com.fujitsu.vdmj.in.definitions.INMultiBindListDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.statements.INSkipStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.traces.StatementIterator;
import com.fujitsu.vdmj.traces.TraceIterator;
import com.fujitsu.vdmj.traces.TraceIteratorList;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Quantifier;
import com.fujitsu.vdmj.values.QuantifierList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

/**
 * A class representing a let-be-st trace binding.
 */
public class INTraceLetBeStBinding extends INTraceDefinition
{
    private static final long serialVersionUID = 1L;
	private static final INSkipStatement SKIP = new INSkipStatement(LexLocation.ANY);
	public final INMultipleBind bind;
	public final INExpression stexp;
	public final INTraceDefinition body;
	public final INMultiBindListDefinition def;

	public INTraceLetBeStBinding(
		LexLocation location, INMultipleBind bind, INExpression stexp, INTraceDefinition body,
		INMultiBindListDefinition def)
	{
		super(location);
		this.bind = bind;
		this.stexp = stexp;
		this.body = body;
		this.def = def;
	}

	@Override
	public String toString()
	{
		return "let " + bind +
			(stexp == null ? "" : " be st " + stexp.toString()) + " in " + body;
	}

	@Override
	public TraceIterator getIterator(Context ctxt)
	{
		TraceIteratorList iterators = new TraceIteratorList();

		try
		{
			QuantifierList quantifiers = new QuantifierList();

			for (INMultipleBind mb: def.bindings)
			{
				ValueList bvals = mb.getBindValues(ctxt, true);		// NB. permuted

				for (INPattern p: mb.plist)
				{
					Quantifier q = new Quantifier(p, bvals);
					quantifiers.add(q);
				}
			}

			quantifiers.init(ctxt, true);

			if (quantifiers.finished())		// No entries at all
			{
				return new StatementIterator(SKIP);
			}

			while (quantifiers.hasNext())
			{
				Context evalContext = new Context(location, "TRACE", ctxt);
				NameValuePairList nvpl = quantifiers.next();
				boolean matches = true;

				for (NameValuePair nvp: nvpl)
				{
					Value v = evalContext.get(nvp.name);

					if (v == null)
					{
						evalContext.put(nvp.name, nvp.value);
					}
					else
					{
						if (!v.equals(nvp.value))
						{
							matches = false;
							break;	// This quantifier set does not match
						}
					}
				}

				if (matches &&
					(stexp == null || stexp.eval(evalContext).boolValue(ctxt)))
				{
					TraceIterator iter = body.getIterator(evalContext);
					iter.setVariables(new INTraceVariableList(evalContext, def.defs));
					iterators.add(iter);
				}
			}
		}
        catch (ValueException e)
        {
        	throw new ContextException(e, location);
        }

		return iterators.getAlternatveIterator();
	}
}
