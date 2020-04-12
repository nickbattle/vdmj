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

	public static void error(Exception e)
	{
		printf("EXCEPTION: %s", e.getMessage());
	}
	
	public static void dump(List<VDMMessage> messages)
	{
		for (VDMMessage m: messages)
		{
			Log.printf("MSG: %s", m.toString());
		}
	}
}
