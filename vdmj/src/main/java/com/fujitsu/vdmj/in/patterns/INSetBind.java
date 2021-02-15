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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.visitors.INBindVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class INSetBind extends INBind
{
	private static final long serialVersionUID = 1L;
	public final INExpression set;

	public INSetBind(INPattern pattern, INExpression set)
	{
		super(pattern.location, pattern);
		this.set = set;
	}

	@Override
	public INMultipleBindList getMultipleBindList()
	{
		INPatternList plist = new INPatternList();
		plist.add(pattern);
		INMultipleBindList mblist = new INMultipleBindList();
		mblist.add(new INMultipleSetBind(plist, set));
		return mblist;
	}

	@Override
	public String toString()
	{
		return pattern + " in set " + set;
	}

	@Override
	public ValueList getBindValues(Context ctxt, boolean permuted) throws ValueException
	{
		ValueList results = new ValueList();
		ValueSet elements = set.eval(ctxt).setValue(ctxt);
		elements.sort();

		for (Value e: elements)
		{
			Value d = e.deref();

			if (d instanceof SetValue && permuted)
			{
				SetValue sv = (SetValue)d;
				results.addAll(sv.permutedSets());
			}
			else
			{
				results.add(e);
			}
		}

		return results;
	}

	@Override
	public <R, S> R apply(INBindVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetBind(this, arg);
	}
}
