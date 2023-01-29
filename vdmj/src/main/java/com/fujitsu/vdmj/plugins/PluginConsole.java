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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.messages.Console;

public class PluginConsole
{
	public static boolean quiet = false;
	
	public static void fail(String reason)
	{
		println(reason);
		System.exit(1);
	}

	public static void verbose(String message)
	{
		if (Settings.verbose)
		{
			println(message);
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

	public static void infoln(String m)
	{
		if (!quiet)
		{
			Console.out.println(m);
		}
	}

	public static void println(String m)
	{
		Console.out.println(m);
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
}
