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

import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import com.fujitsu.vdmj.ExitStatus;
import com.fujitsu.vdmj.Settings;

public class SetCommand extends ControlAnalysisCommand
{
	private final static String USAGE = "Usage: set [<pre|post|inv|dtc|exceptions|measures|annotations> <on|off>]";

	public SetCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("set"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	private void isEnabled(String name, boolean flag)
	{
		printf("%s are %s\n", name, flag ? "enabled" : "disabled");
	}

	@Override
	public String run(String line)
	{
		if (argv.length == 1)
		{
			isEnabled("Preconditions", Settings.prechecks);
			isEnabled("Postconditions", Settings.postchecks);
			isEnabled("Invariants", Settings.invchecks);
			isEnabled("Dynamic type checks", Settings.dynamictypechecks);
			isEnabled("Pre/post/inv exceptions", Settings.exceptions);
			isEnabled("Measure checks", Settings.measureChecks);
			isEnabled("Annotations", Settings.annotations);
			
			return null;
		}
		else
		{
			if (argv.length == 3 &&
				(argv[2].equalsIgnoreCase("on") || argv[2].equalsIgnoreCase("off")))
			{
				boolean setting = argv[2].equalsIgnoreCase("on");

	    		if (argv[1].equals("pre"))
	    		{
	    			Settings.prechecks = setting;
	    		}
	    		else if (argv[1].equals("post"))
	    		{
	    			Settings.postchecks = setting;
	    		}
	    		else if (argv[1].equals("inv"))
	    		{
	    			Settings.invchecks = setting;
	    		}
	    		else if (argv[1].equals("dtc"))
	    		{
	    			// NB. Do both
	    			Settings.invchecks = setting;
	    			Settings.dynamictypechecks = setting;
	    		}
	    		else if (argv[1].equals("exceptions"))
	    		{
	    			Settings.exceptions = setting;
	    		}
	    		else if (argv[1].equals("measures"))
	    		{
	    			if (setting != Settings.annotations)
	    			{
		    			println("Specification will now be re-parsed (reloaded)");
		    			exitStatus = ExitStatus.RELOAD;
		    			carryOn = false;
	    			}

	    			Settings.measureChecks = setting;
	    		}
	    		else if (argv[1].equals("annotations"))
	    		{
	    			if (setting != Settings.annotations)
	    			{
		    			println("Specification will now be re-parsed (reloaded)");
		    			exitStatus = ExitStatus.RELOAD;
		    			carryOn = false;
	    			}

	    			Settings.annotations = setting;
	    		}
				else
				{
					return USAGE;
				}
	    		
	    		return null;
			}
			else
			{
				return USAGE;
			}
		}
	}
	
	public static String help()
	{
		return "set [<pre|post|inv|dtc|measures|annotations> <on|off>] - set runtime checks";
	}
}
