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

package com.fujitsu.vdmj.util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Utils
{
	public static <T> String listToString(List<T> list)
	{
		return listToString("", list, ", ", "");
	}

	public static <T> String listToString(List<T> list, String separator)
	{
		return listToString("", list, separator, "");
	}

	public static <T> String listToString(String before, List<T> list, String separator, String after)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(before);

		if (!list.isEmpty())
		{
			sb.append(list.get(0).toString());

			for (int i=1; i<list.size(); i++)
			{
				sb.append(separator);
				sb.append(list.get(i).toString());
			}
		}

		sb.append(after);
		return sb.toString();
	}

	public static <T> String setToString(Set<T> set, String separator)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(");

		if (!set.isEmpty())
		{
			Iterator<T> i = set.iterator();
			sb.append(i.next().toString());

			while (i.hasNext())
			{
				sb.append(separator);
				sb.append(i.next().toString());
			}
		}

		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Clean an expression has outer brackets. We have to check
	 * for cases like "(a + 1) * (b + 1)", which look outer-bracketed.
	 */
	public static String deBracketed(Object object)
	{
		String arg = object.toString();
		
		if (arg.startsWith("(") && arg.endsWith(")"))
		{
			int count = 0;
			int i=0;
			
			while (i < arg.length())
			{
				char c = arg.charAt(i);
				
				if (c == '(')
				{
					count++;
				}
				else if (c == ')')
				{
					count--;
					if (count == 0) break;
				}
				
				i++;
			}
			
			if (i == arg.length() - 1)	// ie. match of first "(" is last char.
			{
				return arg.substring(1, arg.length() - 1);	// eg. "(x + y)" is "x + y"
			}
			else
			{
				return arg;
			}
		}
		
		return arg;
	}
}
