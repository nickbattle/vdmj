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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.PluginConsole;
import com.fujitsu.vdmj.pog.POStatus;

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
	private static List<POStatus> includes = new Vector<POStatus>();
	
	public static void setQuiet(boolean quiet)
	{
		QCConsole.quiet = quiet;
	}
	
	public static boolean getQuiet()
	{
		return quiet;
	}
	
	public static void setIncludes(List<POStatus> includes)
	{
		QCConsole.includes = includes;
	}

	public static void info(POStatus status, String m)
	{
		if (!quiet && (includes.isEmpty() || includes.contains(status)))
		{
			Console.out.print(m);
		}
	}

	public static void infof(POStatus status, String format, Object... args)
	{
		if (!quiet && (includes.isEmpty() || includes.contains(status)))
		{
			Console.out.printf(format, args);
		}
	}

	public static void infoln(POStatus status, Object m)
	{
		if (!quiet && (includes.isEmpty() || includes.contains(status)))
		{
			Console.out.println(m.toString());
		}
	}
}
