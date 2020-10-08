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

import java.util.LinkedHashMap;

public class JSONObject extends LinkedHashMap<String, Object>	// Order preserving!
{
	private static final long serialVersionUID = 1L;
	
	public JSONObject(Object... args)
	{
		if (args.length % 2 != 0)
		{
			throw new IllegalArgumentException("JSONObject requires argument pairs");
		}
		
		for (int a=0; a<args.length; a=a+2)
		{
			if (!(args[a] instanceof String))
			{
				throw new IllegalArgumentException("JSONObject name must be String");
			}
			
			put((String)args[a], args[a+1]);
		}
	}
	
	@Override
	public Object put(String name, Object value)
	{
		if (value instanceof Integer)
		{
			value = ((Integer)value).longValue();
		}
		else if (value instanceof Short)
		{
			value = ((Short)value).longValue();
		}
		
		return super.put(name, value);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T get(String name)
	{
		return (T)super.get(name);
	}
}
