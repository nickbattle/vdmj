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

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.RTValidator;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.HelpList;

public class LogCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: log | validate";

	public LogCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("log") &&
			!argv[0].equals("validate"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (Settings.dialect != Dialect.VDM_RT)
		{
			return "Command is only available in VDM-RT";
		}
		
		switch (argv[0])
		{
			case "log":
				doLog();
				break;
				
			case "validate":
				doValidate();
				break;
		}
		
		return null;
	}
	
	private void doLog()
	{
		if (argv.length == 1)
		{
			if (RTLogger.getLogSize() > 0)
			{
				println("Flushing " + RTLogger.getLogSize() + " RT events");
			}

			try
			{
				RTLogger.setLogfileName(null);
			}
			catch (FileNotFoundException e)
			{
				// ignore
			}
			
			println("RT events now logged to the console");
			return;
		}

		if (argv.length != 2)
		{
			println("Usage: log [<file> | off]");
		}
		else if (argv[1].equals("off"))
		{
			RTLogger.enable(false);
			println("RT event logging disabled");
		}
		else
		{
			try
			{
				RTLogger.setLogfileName(new File(argv[1]));
				println("RT events now logged to " + argv[1]);
			}
			catch (FileNotFoundException e)
			{
				println("Cannot create RT event log: " + e.getMessage());
			}
		}
	}
	
	private void doValidate()
	{
		if (argv.length != 2)
		{
			println("Usage: validate <file>");
			return;
		}
		
		if (!Settings.annotations)
		{
			println("Enable annotations first, using 'set annotations on'");
			return;
		}

		File logfile = null;

		if (argv.length == 2)
		{
			logfile = new File(argv[1]);
		}
		else
		{
			RTLogger.dump(false);
			logfile = RTLogger.getLogfileName();
		}
		
		if (logfile != null)
		{
			try
			{
				println("Validating RT events from " + logfile);
				int errs = RTValidator.validate(logfile);
				println(errs == 0 ? "No errors found" : "Found " + errs + " conjecture failures");
			}
			catch (IOException e)
			{
				println("Error: " + e.getMessage());
			}
		}
		else
		{
			println("No log file given - use 'validate <file>'");
		}
	}
	
	public static HelpList help()
	{
		return new HelpList(
			"log [<file> | off] - log RT events to file",
			"validate <file> - validate RT log events"
		);
	}
}
