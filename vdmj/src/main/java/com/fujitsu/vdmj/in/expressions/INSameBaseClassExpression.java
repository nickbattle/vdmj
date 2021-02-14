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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

public class INSameBaseClassExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression left;
	public final INExpression right;

	public INSameBaseClassExpression(LexLocation start, INExpression left, INExpression right)
	{
		super(start);

		this.left = left;
		this.right = right;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
			Value l = left.eval(ctxt);
			Value r = right.eval(ctxt);

			if (!l.isType(ObjectValue.class) ||
				!r.isType(ObjectValue.class))
			{
				return new BooleanValue(false);
			}

			ObjectValue lv = l.objectValue(ctxt);
			ObjectValue rv = r.objectValue(ctxt);

			TCTypeList lbases = lv.getBaseTypes();
			TCTypeList rbases = rv.getBaseTypes();

			for (TCType ltype: lbases)
			{
				if (rbases.contains(ltype))
				{
					return new BooleanValue(true);
				}
			}

			return new BooleanValue(false);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public String toString()
	{
		return "samebaseclass(" + left + "," + right + ")";
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSameBaseClassExpression(this, arg);
	}
}
