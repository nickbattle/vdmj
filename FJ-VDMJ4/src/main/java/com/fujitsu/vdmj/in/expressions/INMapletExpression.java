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

import java.io.Serializable;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.ValueList;

public class INMapletExpression extends INNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final INExpression left;
	public final INExpression right;

	public INMapletExpression(INExpression left, INExpression right)
	{
		super(left.location);
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString()
	{
		return left + " |-> " + right;
	}

	public ValueList getValues(Context ctxt)
	{
		ValueList list = left.getValues(ctxt);
		list.addAll(right.getValues(ctxt));
		return list;
	}

	public TCNameList getOldNames()
	{
		TCNameList list = left.getOldNames();
		list.addAll(right.getOldNames());
		return list;
	}
}
