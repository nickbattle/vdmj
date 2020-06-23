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

import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Quantifier;
import com.fujitsu.vdmj.values.QuantifierList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;

public class INMapCompExpression extends INMapExpression
{
	private static final long serialVersionUID = 1L;
	public final INMapletExpression first;
	public final INMultipleBindList bindings;
	public final INExpression predicate;

	public INMapCompExpression(LexLocation start, INMapletExpression first, INMultipleBindList bindings,
		INExpression predicate)
	{
		super(start);
		this.first = first;
		this.bindings = bindings;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "{" + first + " | " + Utils.listToString(bindings) +
			(predicate == null ? "}" : " & " + predicate + "}");
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		ValueMap map = new ValueMap();

		try
		{
			QuantifierList quantifiers = new QuantifierList();

			for (INMultipleBind mb: bindings)
			{
				ValueList bvals = mb.getBindValues(ctxt, false);

				for (INPattern p: mb.plist)
				{
					Quantifier q = new Quantifier(p, bvals);
					quantifiers.add(q);
				}
			}

			quantifiers.init(ctxt, false);

			while (quantifiers.hasNext())
			{
				Context evalContext = new Context(location, "map comprehension", ctxt);
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

				try
				{
					if (matches &&
						(predicate == null ||
						 predicate.eval(evalContext).boolValue(ctxt)))
					{
						Value dom = first.left.eval(evalContext);
						Value rng = first.right.eval(evalContext);
						first.location.hit();

						Value old = map.put(dom, rng);

						if (old != null && !old.equals(rng))
						{
							abort(4016, "Duplicate map keys have different values: " + dom, ctxt);
						}
					}
				}
				catch (ValueException e)
				{
					predicate.abort(e);
				}
			}
		}
	    catch (ValueException e)
	    {
	    	return abort(e);
	    }

		return new MapValue(map);
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		found = first.findExpression(lineno);
		if (found != null) return found;

		return predicate == null ? null : predicate.findExpression(lineno);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = first.getValues(ctxt);

		for (INMultipleBind mb: bindings)
		{
			list.addAll(mb.getValues(ctxt));
		}

		if (predicate != null)
		{
			list.addAll(predicate.getValues(ctxt));
		}

		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = first.getOldNames();

		for (INMultipleBind mb: bindings)
		{
			list.addAll(mb.getOldNames());
		}

		if (predicate != null)
		{
			list.addAll(predicate.getOldNames());
		}

		return list;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapCompExpression(this, arg);
	}
}
