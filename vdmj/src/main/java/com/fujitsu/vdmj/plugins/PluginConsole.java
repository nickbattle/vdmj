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

package com.fujitsu.vdmj.plugins;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.messages.Console;

public class PluginConsole
{
	private static boolean quiet = false;
	
	public static void setQuiet(boolean quiet)
	{
		PluginConsole.quiet = quiet;
	}
	
	public static void fail(String format, Object... args)
	{
		Console.out.printf(format + "\n", args);
		System.exit(1);
	}

	public static void verbose(String format, Object... args)
	{
		if (Settings.verbose)
		{
			Console.out.printf(format + "\n", args);
		}
	}

	public static String plural(int n, String s, String pl)
	{
		return n + " " + (n != 1 ? s + pl : s);
	}

	public static void info(String m)
	{
		if (!quiet)
		{
			Console.out.print(m);
		}
	}

	public static void infof(String format, Object... args)
	{
		if (!quiet)
		{
			Console.out.printf(format, args);
		}
	}

	public static void infoln(String m)
	{
		if (!quiet)
		{
			Console.out.println(m);
		}
	}

	public static void println(Object m)
	{
		Console.out.println(m.toString());
	}

	public static void printf(String format, Object... args)
	{
		Console.out.printf(format, args);
	}

	public static void errorln(Object m)
	{
		Console.err.println(m.toString());
	}

	public static void errorf(String format, Object... args)
	{
		Console.err.printf(format, args);
	}

	public static void println(Throwable throwable)
	{
		Console.out.println(String.format("EXCEPTION: %s %s",
				throwable.getClass().getSimpleName(), throwable.getMessage()));
		
		StackTraceElement[] stack = throwable.getStackTrace();
		int max = Properties.diag_max_stack;
		int count = (max == 0 )? stack.length : (stack.length < max) ? stack.length : max;
		
		for (int i=0; i<count; i++)
		{
			StackTraceElement frame = stack[i];
			Console.out.println(String.format("  %s %s at %s line %d",
					frame.getClassName(), frame.getMethodName(), frame.getFileName(), frame.getLineNumber()));
		}
		
		if (count < stack.length)
		{
			Console.out.println((stack.length - count) +
				" more frames available (vdmj.diag.max_stack currently = " +
				Properties.diag_max_stack +")");
		}
	}
	
	public static Charset validateCharset(String cs)
	{
		if (!Charset.isSupported(cs))
		{
			println("Charset " + cs + " is not supported\n");
			println("Available charsets:");
			println("Default = " + Charset.defaultCharset());
			Map<String,Charset> available = Charset.availableCharsets();

			for (Entry<String, Charset> entry: available.entrySet())
			{
				println(entry.getKey() + " " + available.get(entry.getKey()).aliases());
			}

			println("");
			fail("Charset " + cs + " is not supported");
		}

		return Charset.forName(cs);
	}
}
