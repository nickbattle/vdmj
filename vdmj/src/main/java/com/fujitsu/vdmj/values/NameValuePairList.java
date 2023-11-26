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

package com.fujitsu.vdmj.values;

import java.util.Vector;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class NameValuePairList extends Vector<NameValuePair>
{
	public NameValuePairList()
	{
		super();
	}

	public NameValuePairList(NameValuePair nv)
	{
		add(nv);
	}

	public boolean add(TCNameToken name, Value value)
	{
		return super.add(new NameValuePair(name, value));
	}

	@Override
	public boolean add(NameValuePair nv)
	{
		return super.add(nv);
	}

	@SuppressWarnings("unchecked")
	public <T> T getNamedValue(TCNameToken sought)
	{
		for (NameValuePair pair: this)
		{
			if (pair.name.equals(sought))
			{
				return (T) pair.value;
			}
		}
		
		return null;
	}

	public NameValuePairList getUpdatable(ValueListenerList listeners)
	{
		NameValuePairList nlist = new NameValuePairList();

		for (NameValuePair nvp: this)
		{
			nlist.add(nvp.name, nvp.value.getUpdatable(listeners));
		}

		return nlist;
	}

	@Override
	public String toString()
	{
		return Utils.listToString("[", this, ", ", "]");
	}
}
