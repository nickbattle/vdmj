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

package com.fujitsu.vdmj.in.patterns;

import java.util.List;
import java.util.Map.Entry;
import java.util.Vector;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

public class INMapletPattern extends INNode
{
	private static final long serialVersionUID = 1L;
	public final INPattern from;
	public final INPattern to;

	public INMapletPattern(INPattern from, INPattern to)
	{
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return from + " |-> " + to;
	}

	public List<INIdentifierPattern> findIdentifiers()
	{
		List<INIdentifierPattern> list = new Vector<INIdentifierPattern>();

		list.addAll(from.findIdentifiers());
		list.addAll(to.findIdentifiers());

		return list;
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

	public boolean isConstrained()
	{
		if (from.isConstrained() || to.isConstrained())
		{
			return true;
		}

		return (from.getPossibleType() instanceof TCUnionType ||
				to.getPossibleType() instanceof TCUnionType);
	}
	
	public TCType getPossibleType()
	{
		return new TCMapType(from.location, from.getPossibleType(), to.getPossibleType());
	}
}
