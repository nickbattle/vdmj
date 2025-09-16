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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package quickcheck.commands;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.PluginConsole;

/**
 * This extends the PluginConsole from VDMJ to allow us to have "global" quiet
 * setting within the QC command environment, without affecting the outer
 * environment. By extending PluginConsole, we get a "private" quiet flag
 * that won't interfere with the global one, but we can still use the inherited
 * methods as well.
 */
public class QCConsole extends PluginConsole
{
	private static boolean quiet = false;
	private static boolean verbose = false;
	
	public static void setQuiet(boolean quiet)
	{
		QCConsole.quiet = quiet;
	}
	
	public static boolean getQuiet()
	{
		return quiet;
	}
	
	public static void setVerbose(boolean verbose)
	{
		QCConsole.verbose = verbose;
	}
	
	public static boolean getVerbose()
	{
		return verbose;
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

	public static void infoln(Object m)
	{
		if (!quiet)
		{
			Console.out.println(m.toString());
		}
	}

	public static void verbose(String format, Object... args)
	{
		if (!quiet && verbose)
		{
			Console.out.printf(format, args);
		}
	}

	public static void verboseln(String m)
	{
		if (!quiet && verbose)
		{
			Console.out.println(m);
		}
	}
}
