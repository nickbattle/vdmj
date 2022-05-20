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

package com.fujitsu.vdmj.messages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import com.fujitsu.vdmj.scheduler.SystemClock;

public class RTLogger
{
	private static boolean enabled = false;
	private static List<String> events = new LinkedList<String>();
	private static File logfile;
	private static PrintWriter writer = null;
	private static String cached = null;

	public static synchronized void enable(boolean on)
	{
		if (!on)
		{
			dump(true);
			cached = null;
		}

		enabled = on;
	}
	
	public static synchronized boolean isEnabled()
	{
		return enabled;
	}

	public static synchronized void log(String event)
	{
		if (!enabled)
		{
			return;
		}

		event = event + " time: " + SystemClock.getWallTime();

		if (event.startsWith("ThreadSwapIn") ||
			event.startsWith("DelayedThreadSwapIn"))
		{
			if (cached != null)
			{
				doLog(cached);
			}

			cached = event;
			return;
		}

		if (cached != null)
		{
			if (event.startsWith("ThreadSwapOut"))
			{
				String[] cparts = cached.split("\\s+");
				String[] eparts = event.split("\\s+");

				if (cparts[0].equals("DelayedThreadSwapIn") &&
					cparts[3].equals(eparts[3]) &&	// thread
					cparts[15].equals(eparts[13]))	// time
				{
					cached = null;
					return;
				}

				if (cparts[0].equals("ThreadSwapIn") &&
					cparts[3].equals(eparts[3]) &&	// thread
					cparts[13].equals(eparts[13]))	// time
				{
					cached = null;
					return;
				}
			}

			doLog(cached);
			cached = null;
		}

		doLog(event);
	}

	private static void doLog(String event)
	{
		if (writer == null)
		{
			Console.out.println(event);
		}
		else
		{
    		events.add(event);

    		if (events.size() > 1000)
    		{
    			dump(false);
    		}
		}
	}

	public static void setLogfile(PrintWriter out)
	{
		enabled = true;
		dump(true);		// Write out and close previous
		writer = out;
		cached = null;
	}

	public static void setLogfileName(File file) throws FileNotFoundException
	{
		logfile = file;
		PrintWriter p = new PrintWriter(new FileOutputStream(file, false));
		setLogfile(p);
	}

	public static int getLogSize()
	{
		return events.size();
	}
	
	public static File getFile()
	{
		return logfile;
	}

	public static synchronized void dump(boolean close)
	{
		if (writer != null)
		{
    		for (String event: events)
    		{
    			writer.println(event);
    		}

    		writer.flush();
    		events.clear();

    		if (close)
    		{
    			writer.close();
    			writer = null;
    			cached = null;
    		}
		}
	}
}
