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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INElseIfExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression elseIfExp;
	public final INExpression thenExp;

	public INElseIfExpression(LexLocation location,
			INExpression elseIfExp, INExpression thenExp)
	{
		super(location);
		this.elseIfExp = elseIfExp;
		this.thenExp = thenExp;
	}

	@Override
	public String toString()
	{
		return "elseif " + elseIfExp + "\nthen " + thenExp;
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return thenExp.findExpression(lineno);
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			return elseIfExp.eval(ctxt).boolValue(ctxt) ? thenExp.eval(ctxt) : null;
		}
        catch (ValueException e)
        {
        	return abort(e);
        }
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = elseIfExp.getValues(ctxt);
		list.addAll(thenExp.getValues(ctxt));
		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = elseIfExp.getOldNames();
		list.addAll(thenExp.getOldNames());
		return list;
	}

	@Override
	public INExpressionList getSubExpressions()
	{
		INExpressionList subs = elseIfExp.getSubExpressions();
		subs.addAll(thenExp.getSubExpressions());
		subs.add(this);
		return subs;
	}
}
