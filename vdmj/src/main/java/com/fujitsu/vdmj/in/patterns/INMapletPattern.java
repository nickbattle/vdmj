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

import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import com.fujitsu.vdmj.in.patterns.visitors.INPatternVisitor;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

public class INMapletPattern extends INPattern
{
	private static final long serialVersionUID = 1L;
	public final INPattern from;
	public final INPattern to;

	public INMapletPattern(INPattern from, INPattern to)
	{
		super(from.location);
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return from + " |-> " + to;
	}

	public List<NameValuePairList> getAllNamedValues(Entry<Value, Value> maplet, Context ctxt)
		throws PatternMatchException
	{
		List<NameValuePairList> flist = from.getAllNamedValues(maplet.getKey(), ctxt);
		List<NameValuePairList> tlist = to.getAllNamedValues(maplet.getValue(), ctxt);
		List<NameValuePairList> results = new Vector<NameValuePairList>();

		for (NameValuePairList f: flist)
		{
			for (NameValuePairList t: tlist)
			{
				NameValuePairList both = new NameValuePairList();
				both.addAll(f);
				both.addAll(t);
				results.add(both);	// Every combination of from/to mappings
			}
		}

		return results;
	}

	@Override
	public boolean isConstrained()
	{
		if (from.isConstrained() || to.isConstrained())
		{
			return true;
		}

		return (from.getPossibleType() instanceof TCUnionType ||
				to.getPossibleType() instanceof TCUnionType);
	}

	@Override
	public List<NameValuePairList> getAllNamedValues(Value expval, Context ctxt) throws PatternMatchException
	{
		throw new InternalException(0075, "Maplet getAllNamedValues called");	// See method above
	}

	@Override
	public <R, S> R apply(INPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapletPattern(this, arg);
	}
}
