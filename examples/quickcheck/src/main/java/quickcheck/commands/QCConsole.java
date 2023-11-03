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

package quickcheck.commands;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.messages.Console;

/**
 * This copies the PluginConsole from VDMJ to allow us to have "global" quiet
 * setting within the QC command environment, without affecting the outer
 * environment.
 */

public class QCConsole
{
	private static boolean quiet = false;
	
	public static void setQuiet(boolean quiet)
	{
		QCConsole.quiet = quiet;
	}
	
	public static boolean getQuiet()
	{
		return quiet;
	}
	
	public static void verbose(String format, Object... args)
	{
		if (Settings.verbose)
		{
			Console.out.printf(format, args);
		}
	}

	public static void verboseln(String m)
	{
		if (Settings.verbose)
		{
			Console.out.println(m);
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
}
