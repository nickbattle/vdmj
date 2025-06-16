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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj.commands;

import com.fujitsu.vdmj.Settings;
import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;

public class SetCommand extends AnalysisCommand
{
	public static final String USAGE = "Usage: set [<pre|post|inv|dtc|measures|annotations> <on|off>]";
	public static final String HELP = "set [<pre|post|inv|dtc|measures|annotations> <on|off>] - set runtime checks";
	
	private String option = null;
	private Boolean setting = null;

	public SetCommand(String line)
	{
		super(line);
		
		if (argv.length == 1)
		{
			this.option = "set";
		}
		else if (argv.length == 3 &&
			(argv[2].equalsIgnoreCase("on") || argv[2].equalsIgnoreCase("off")))
		{
			this.option = argv[1];
			this.setting = argv[2].equalsIgnoreCase("on");
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	private StringBuilder sb;
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		sb = new StringBuilder();
		boolean changed = false;
		
		switch (option)
		{
			case "set":
				isEnabled("Preconditions", Settings.prechecks);
				isEnabled("Postconditions", Settings.postchecks);
				isEnabled("Invariants", Settings.invchecks);
				isEnabled("Dynamic type checks", Settings.dynamictypechecks);
				isEnabled("Pre/post/inv exceptions", Settings.exceptions);
				isEnabled("Measure checks", Settings.measureChecks);
				isEnabled("Annotations", Settings.annotations);
				break;
				
    		case "pre":
    			Settings.prechecks = setting;
				isEnabled("Preconditions", Settings.prechecks);
    			break;
    			
    		case "post":
    			Settings.postchecks = setting;
				isEnabled("Postconditions", Settings.postchecks);
    			break;

    		case "inv":
    			Settings.invchecks = setting;
				isEnabled("Invariants", Settings.invchecks);
    			break;
    			
    		case "dtc":
    			// NB. Do both
    			Settings.invchecks = setting;
    			Settings.dynamictypechecks = setting;
				isEnabled("Invariants", Settings.invchecks);
				isEnabled("Dynamic type checks", Settings.dynamictypechecks);
    			break;
    			
    		case "exceptions":
    			Settings.exceptions = setting;
				isEnabled("Pre/post/inv exceptions", Settings.exceptions);
    			break;
    			
    		case "measures":
    			changed = (setting != Settings.measureChecks);
    			Settings.measureChecks = setting;
				isEnabled("Measure checks", Settings.measureChecks);

				if (changed)
    			{
	    			sb.append("Specification must now be restarted to take effect");
    			}
    			break;
    			
    		case "annotations":
    			changed = (setting != Settings.annotations);
    			Settings.annotations = setting;
				isEnabled("Annotations", Settings.annotations);

				if (changed)
    			{
	    			sb.append("Specification must now be restarted to take effect");
    			}
    			break;
    			
    		default:
    			sb.append(USAGE);
    			break;
		}
		
		return new DAPMessageList(request, new JSONObject("result", sb.toString()));
	}
	
	private void isEnabled(String name, boolean flag)
	{
		sb.append(name + (flag ? " are enabled" : " are disabled") + "\n");
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}
}
