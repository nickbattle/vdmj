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

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.tc.patterns.TCNamePatternPair;
import com.fujitsu.vdmj.tc.patterns.TCNamePatternPairList;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

@SuppressWarnings("serial")
public class INNamePatternPairList extends INMappedList<TCNamePatternPair, INNamePatternPair>
{
	public INNamePatternPairList(TCNamePatternPairList from) throws Exception
	{
		super(from);
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public NameValuePairList getNamedValues(Value value, Context ctxt)
		throws PatternMatchException
	{
		NameValuePairList list = new NameValuePairList();

		for (INNamePatternPair npp: this)
		{
			list.addAll(npp.pattern.getNamedValues(value, ctxt));
		}

		return list;
	}

	public boolean isConstrained()
	{
		for (INNamePatternPair npp: this)
		{
			if (npp.pattern.isConstrained()) return true;		// NB. OR
		}

		return false;
	}
}
