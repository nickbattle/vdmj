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

package vdmj.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.RTLogger;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;

public class LogCommand extends Command
{
	public static final String USAGE = "Usage: log [<file> | off]";
	public static final String[] HELP =	{ "log", "log [<file> | off] - control RT logging" };
	
	private File logfile = null;
	
	public LogCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length == 2)
		{
			if (parts[1].equals("off"))
			{
				logfile = null;
			}
			else
			{
				logfile = new File(parts[1]);
			}
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		String message = null;
		
		if (Settings.dialect != Dialect.VDM_RT)
		{
			return new DAPMessageList(request,
					false, "Command only available for VDM-RT", null);			
		}

		if (logfile == null)
		{
			RTLogger.enable(false);
			message = "RT event logging disabled";
		}
		else
		{
			try
			{
				PrintWriter p = new PrintWriter(new FileOutputStream(logfile, false));
				RTLogger.setLogfile(p);
				message = "RT events now logged to " + logfile;
			}
			catch (FileNotFoundException e)
			{
				 message = "Cannot create RT event log: " + e.getMessage();
			}
		}
		
		return new DAPMessageList(request, new JSONObject("result", message));
	}
	
	@Override
	public boolean notWhenRunning()
	{
		return true;
	}
}
