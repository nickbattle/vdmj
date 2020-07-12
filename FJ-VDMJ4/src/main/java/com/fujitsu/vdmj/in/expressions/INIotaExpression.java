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

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.patterns.INBind;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INIotaExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INBind bind;
	public final INExpression predicate;

	public INIotaExpression(LexLocation location, INBind bind, INExpression predicate)
	{
		super(location);
		this.bind = bind;
		this.predicate = predicate;
	}

	@Override
	public String toString()
	{
		return "(iota " + bind + " & " + predicate + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		ValueList allValues = null;
		Value result = null;

		try
		{
			allValues = bind.getBindValues(ctxt, false);
		}
		catch (ValueException e)
		{
			abort(e);
		}

		for (Value val: allValues)
		{
			try
			{
				Context evalContext = new Context(location, "iota", ctxt);
				evalContext.putList(bind.pattern.getNamedValues(val, ctxt));

				if (predicate.eval(evalContext).boolValue(ctxt))
				{
					if (result != null && !result.equals(val))
					{
						abort(4013, "Iota selects more than one result", ctxt);
					}

					result = val;
				}
			}
	        catch (ValueException e)
	        {
	        	predicate.abort(e);
	        }
			catch (PatternMatchException e)
			{
				// Ignore pattern mismatches
			}
	    }

		if (result != null)
		{
			return result;
		}

		return abort(4014, "Iota does not select a result", ctxt);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = bind.getValues(ctxt);
		list.addAll(predicate.getValues(ctxt));
		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = bind.getOldNames();
		list.addAll(predicate.getOldNames());
		return list;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIotaExpression(this, arg);
	}
}
