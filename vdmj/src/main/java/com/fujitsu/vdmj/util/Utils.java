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

import java.net.JarURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.VDMJ;

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
	
	public static String[] toArgv(String s)
	{
		List<String> list = new Vector<String>();
		int p = 0;
		boolean quoting = false;
		boolean backslash = false;
		StringBuilder sb = new StringBuilder();
		
		while (p < s.length())
		{
			char c = s.charAt(p++);
			
			if (backslash)
			{
				sb.append(c);
				backslash = false;
				continue;
			}
			
			switch (c)
			{
				case '\"':
				case '\'':
					quoting = !quoting;
					
					if (!quoting)
					{
						list.add(sb.toString());
						sb.setLength(0);
					}
					break;

				case '\\':
					backslash = true;
					break;
					
				case ' ':
				case '\t':
					if (quoting)
					{
						sb.append(c);
					}
					else if (sb.length() > 0)
					{
						list.add(sb.toString());
						sb.setLength(0);
					}
					break;
					
				default:
					sb.append(c);
			}
		}
		
		if (sb.length() > 0)
		{
			list.add(sb.toString());
		}
		
		return list.toArray(new String[0]);
	}
	
	public static long mapperStats(long start, String mappings)
	{
		if (Settings.verbose)
		{
    		long now = System.currentTimeMillis();
    		ClassMapper mapper = ClassMapper.getInstance(mappings);
    		long count = mapper.getNodeCount();
    		long load = mapper.getLoadTime();
    		
    		if (load != 0)
    		{
    			Console.out.println("Loaded " + mappings + " in " + (double)load/1000 + " secs");
    		}
    		
    		double time = (double)(now-start-load)/1000;
    		
    		if (time < 0.01)
    		{
    			Console.out.println("Mapped " + count + " nodes with " + mappings + " in " + time + " secs");
    		}
    		else
    		{
    			int rate = (int) (count/time);
    			Console.out.println("Mapped " + count + " nodes with " + mappings + " in " + time + " secs (" + rate + "/sec)");
    		}
    		
    		return System.currentTimeMillis();		// ie. remove load times
		}
		else
		{
			return start;
		}
	}

	public static String getVersion()
	{
		try
		{
			String path = VDMJ.class.getName().replaceAll("\\.", "/");
			URL url = VDMJ.class.getResource("/" + path + ".class");
			JarURLConnection conn = (JarURLConnection)url.openConnection();
		    JarFile jar = conn.getJarFile();
			Manifest mf = jar.getManifest();
			String version = (String)mf.getMainAttributes().get(Attributes.Name.IMPLEMENTATION_VERSION);
			return version;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
