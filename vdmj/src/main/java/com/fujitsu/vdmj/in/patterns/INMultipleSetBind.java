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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.visitors.INMultipleBindVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class INMultipleSetBind extends INMultipleBind
{
	private static final long serialVersionUID = 1L;
	public final INExpression set;

	public INMultipleSetBind(INPatternList plist, INExpression set)
	{
		super(plist);
		this.set = set;
	}

	@Override
	public String toString()
	{
		return plist + " in set " + set;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted)
	{
		try
		{
			ValueList vl = new ValueList();
			ValueSet vs = set.eval(ctxt).setValue(ctxt);
			vs.sort();

			for (Value v: vs)
			{
				Value d = v.deref();

				if (d instanceof SetValue && permuted)
				{
					SetValue sv = (SetValue)d;
					vl.addAll(sv.permutedSets());
				}
				else
				{
					vl.add(v);
				}
			}

			return vl;
		}
		catch (ValueException e)
		{
			abort(e);
			return null;
		}
	}

	@Override
	public <R, S> R apply(INMultipleBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMultipleSetBind(this, arg);
	}
}
