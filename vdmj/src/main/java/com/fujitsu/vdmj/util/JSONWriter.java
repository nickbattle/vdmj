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

package com.fujitsu.vdmj.util;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A very simple JSON format writer, printing Map&lt;String, Object&gt;.
 */
public class JSONWriter
{
	private PrintWriter writer;
	
	public JSONWriter(PrintWriter writer)
	{
		this.writer = writer;
	}
	
	public void flush()
	{
		writer.flush();
	}

	public void writeObject(Map<String, Object> object)
	{
		writer.print("{ ");
		String sep = "";
		
		for (Entry<String, Object> field: object.entrySet())
		{
			writer.print(sep);
			writeString(field.getKey());
			writer.print(" : ");
			writeValue(field.getValue());
			sep = ", ";
		}
		
		writer.print(" }");
	}
	
	private void writeArray(Object[] value)
	{
		writer.print("[ ");
		String sep = "";
		
		for (Object field: value)
		{
			writer.print(sep);
			writeValue(field);
			sep = ", ";
		}
		
		writer.print(" ]");
	}

	private void writeString(String value)
	{
		writer.print('"');
		StringBuilder sb = new StringBuilder();
		
		for (char c: value.toCharArray())
		{
			switch (c)
			{
    		    case '\r':	sb.append("\\r"); break;
    		    case '\n':	sb.append("\\n"); break;
    		    case '\t':	sb.append("\\t"); break;
    		    case '\f':	sb.append("\\f"); break;
    		    case '\b':	sb.append("\\b"); break;
    		    case '\u000B':	sb.append("\\v"); break;

    		    case '\"':	sb.append("\\\""); break;
    		    case '\\':	sb.append("\\\\"); break;

    		    default:
    		    	sb.append(c);
			}
		}
		
		writer.print(sb);
		writer.print('"');
	}

	@SuppressWarnings("unchecked")
	private void writeValue(Object value)
	{
		if (value == null)
		{
			writer.print("null");
		}
		else if (value instanceof Number || value instanceof Boolean)
		{
			writer.print(value);
		}
		else if (value instanceof String)
		{
			writeString((String) value);
		}
		else if (value instanceof Object[])
		{
			writeArray((Object[]) value);
		}
		else if (value instanceof Map)
		{
			writeObject((Map<String, Object>) value);
		}
		else
		{
			throw new RuntimeException("Unexpected object type: " + value.getClass().getSimpleName());
		}
	}
}
