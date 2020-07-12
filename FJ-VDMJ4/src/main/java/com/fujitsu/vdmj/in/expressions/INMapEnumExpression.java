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
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueMap;

public class INMapEnumExpression extends INMapExpression
{
	private static final long serialVersionUID = 1L;
	public final INMapletExpressionList members;

	public INMapEnumExpression(LexLocation location, INMapletExpressionList members)
	{
		super(location);
		this.members = members;
	}

	@Override
	public String toString()
	{
		if (members.isEmpty())
		{
			return "{|->}";
		}
		else
		{
			return "{" + Utils.listToString(members) + "}";
		}
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		ValueMap map = new ValueMap();

		for (INMapletExpression e: members)
		{
			Value l = e.left.eval(ctxt);
			Value r = e.right.eval(ctxt);
			e.location.hit();
			Value old = map.put(l, r);

			if (old != null && !old.equals(r))
			{
				abort(4017, "Duplicate map keys have different values: " + l, ctxt);
			}
		}

		return new MapValue(map);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = new ValueList();

		for (INMapletExpression maplet: members)
		{
			list.addAll(maplet.getValues(ctxt));
		}

		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = new TCNameList();

		for (INMapletExpression maplet: members)
		{
			list.addAll(maplet.getOldNames());
		}

		return list;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapEnumExpression(this, arg);
	}
}
