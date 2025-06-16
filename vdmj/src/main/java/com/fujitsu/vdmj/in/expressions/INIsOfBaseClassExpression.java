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

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

public class INIsOfBaseClassExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken baseclass;
	public final INExpression exp;

	public INIsOfBaseClassExpression(LexLocation start, TCNameToken classname, INExpression exp)
	{
		super(start);

		this.baseclass = classname.getExplicit(false);
		this.exp = exp;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		baseclass.getLocation().hit();

		try
		{
			Value v = exp.eval(ctxt).deref();

			if (!(v instanceof ObjectValue))
			{
				return new BooleanValue(false);
			}

			ObjectValue ov = v.objectValue(ctxt);
			return new BooleanValue(search(ov));
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	private boolean search(ObjectValue from)
	{
		if (from.type.name.getName().equals(baseclass.getName()) &&
			from.superobjects.isEmpty())
		{
			return true;
		}

		for (ObjectValue svalue: from.superobjects)
		{
			if (search(svalue))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString()
	{
		return "isofbaseclass(" + baseclass + "," + exp + ")";
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIsOfBaseClassExpression(this, arg);
	}
}
