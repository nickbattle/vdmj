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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
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
		if (value instanceof Integer)
		{
			value = ((Integer)value).longValue();
		}
		else if (value instanceof Short)
		{
			value = ((Short)value).longValue();
		}
		
		return super.add(value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T index(int i)
	{
		return (T)super.get(i);
	}
}
