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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import com.fujitsu.vdmj.messages.VDMMessage;

import json.JSONObject;

public class Log
{
	private static PrintStream out = null;
	
	public static boolean logging()
	{
		return out != null;
	}
	
	public static void init()
	{
		String filename = System.getProperty("log.filename");
		
		if (filename != null)
		{
			try
			{
				out = new PrintStream(new FileOutputStream(filename, true));	// append
			}
			catch (FileNotFoundException e)
			{
				System.err.printf("Cannot create log file: %s\n", filename);
			}
		}
	}

	public static void init(PrintStream stream)
	{
		out = stream;
	}

	public synchronized static void printf(String format, Object... args)
	{
		if (out != null)
		{
			Calendar now = new GregorianCalendar();
			out.printf("%02d:%02d:%02d.%03d: ",
					now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
					now.get(Calendar.SECOND), now.get(Calendar.MILLISECOND));

			out.printf(format, args);
			out.print("\n");
			out.flush();
		}
	}
	
	public static void error(String format, Object... args)
	{
		printf("ERROR: " + format, args);
	}

	public static void error(Throwable throwable)
	{
		printf("EXCEPTION: %s %s", throwable.getClass().getSimpleName(), throwable.getMessage());
	}
	
	public static void dump(List<VDMMessage> messages)
	{
		for (VDMMessage m: messages)
		{
			Log.printf("MSG: %s", m.toString());
		}
	}
	
	public static void dumpEdit(JSONObject range, StringBuilder buffer)
	{
		if (logging())
		{
			JSONObject position = range.get("start");
			long line = position.get("line");
			long count = 0;
			int start = 0;
			
			while (count < line)
			{
				if (buffer.charAt(start++) == '\n')
				{
					count++;
				}
			}
			
			int end = start;
			while (end < buffer.length() && buffer.charAt(end) != '\n') end++;
			Log.printf("EDITED %d: [%s]", line+1, buffer.substring(start, end));
		}
	}
}
