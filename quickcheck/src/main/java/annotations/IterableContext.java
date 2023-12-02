/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package annotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.Value;

/**
 * A Context that is backed by a list of maps, which can be iterated through.
 * This is used in QuickCheck to load a context with one of a selection of
 * ParameterType settings. 
 */
public class IterableContext extends Context
{
	private static final long serialVersionUID = 1L;
	private final List<Map<TCNameToken, Value>> maps;
	private int nextMap = 0;

	public IterableContext(LexLocation location, String title, Context outer)
	{
		super(location, title, outer);
		maps = new Vector<Map<TCNameToken, Value>>();
	}
	
	public Map<TCNameToken, Value> newMap(int index)
	{
		if (index < maps.size())
		{
			return maps.get(index);
		}
		else
		{
			Map<TCNameToken, Value> values = new HashMap<TCNameToken, Value>(); 
			maps.add(values);
			return values;
		}
	}

	public boolean hasNext()
	{
		return nextMap < maps.size();
	}
	
	public void next()
	{
		this.clear();
		this.putAll(maps.get(nextMap));
		nextMap++;
	}

	public void setDefaults(TCNameToken name, Value value)
	{
		for (Map<TCNameToken, Value> map: maps)
		{
			if (!map.containsKey(name))
			{
				map.put(name, value);
			}
		}
	}
}
