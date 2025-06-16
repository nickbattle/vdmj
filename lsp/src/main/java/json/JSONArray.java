/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package json;

import java.util.Vector;

public class JSONArray extends Vector<Object>
{
	private static final long serialVersionUID = 1L;

	public JSONArray(Object... args)
	{
		for (Object arg: args)
		{
			add(arg);
		}
	}
	
	@Override
	public boolean add(Object value)
	{
		return super.add(JSONValue.validate(value));
	}
	
	@SuppressWarnings("unchecked")
	public <T> T index(int i)
	{
		return (T)super.get(i);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPath(String dotName)
	{
		int dot = dotName.indexOf('.');
		String part = (dot == -1) ? dotName : dotName.substring(0, dot);
		String tail = (dot == -1) ? null : dotName.substring(dot + 1);

		if (part.matches("\\[\\d+\\]"))
		{
			int index = Integer.parseInt(part.substring(1, part.length() - 1));
			
			if (index < size())
			{
				Object obj = get(index);
				
				if (tail == null)
				{
					return (T)obj;
				}
				else if (obj instanceof JSONObject)
				{
					JSONObject json = (JSONObject)obj;
					return (T)json.getPath(tail);
				}
				else if (obj instanceof JSONArray)
				{
					JSONArray json = (JSONArray)obj;
					return (T)json.getPath(tail);
				}
			}
		}

		return null;
	}
}
