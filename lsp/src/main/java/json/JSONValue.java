/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

/**
 * A class to create valid JSON values. These can be added to JSONObject or JSONArray.
 */
public class JSONValue
{
	public static Object validate(Object value)
	{
		if (value == null)
		{
			// fine
		}
		else if (value instanceof Double ||
				 value instanceof Float)
		{
			value = ((Number)value).doubleValue();
		}
		else if (value instanceof Number)
		{
			value = ((Number)value).longValue();
		}
		else if (value instanceof String ||
				 value instanceof Boolean ||
				 value instanceof JSONArray ||
				 value instanceof JSONObject)
		{
			// fine
		}
		else
		{
			throw new IllegalArgumentException("Unexpected JSON value: " + value.getClass().getSimpleName());
		}
		
		return value;
	}
}
