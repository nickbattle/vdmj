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

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INVariableExpression extends INExpression
{
	private static final long serialVersionUID = 1L;

	public final TCNameToken name;

	public INVariableExpression(TCNameToken name)
	{
		super(name.getLocation());
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name.toString();
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		return ctxt.lookup(name);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		Value v = ctxt.check(name);

		if (v == null || !(v instanceof UpdatableValue))
		{
			return new ValueList();
		}
		else
		{
			return new ValueList(v);
		}
	}

	@Override
	public TCNameList getOldNames()
	{
		if (name.isOld())
		{
			return new TCNameList(name);
		}
		else
		{
			return new TCNameList();
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseVariableExpression(this, arg);
	}
}
