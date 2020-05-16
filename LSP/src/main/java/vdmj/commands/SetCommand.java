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

package vdmj.commands;

import com.fujitsu.vdmj.Settings;
import dap.DAPMessageList;
import dap.DAPRequest;

public class SetCommand extends Command
{
	public static final String USAGE = "Usage: set [<pre|post|inv|dtc|measures|annotations> <on|off>]";
	public static final String[] HELP =	{ "set", "set [<pre|post|inv|dtc|measures|annotations> <on|off>] - set runtime checks" };
	
	private String option = null;
	private Boolean setting = null;

	public SetCommand(String line) throws IllegalArgumentException
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length == 1)
		{
			this.option = "set";
		}
		else if (parts.length == 3 &&
			(parts[2].equalsIgnoreCase("on") || parts[2].equalsIgnoreCase("off")))
		{
			this.option = parts[1];
			this.setting = parts[2].equalsIgnoreCase("on");
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
    			break;
    			
    		case "post":
    			Settings.postchecks = setting;
    			break;

    		case "inv":
    			Settings.invchecks = setting;
    			break;
    			
    		case "dtc":
    			// NB. Do both
    			Settings.invchecks = setting;
    			Settings.dynamictypechecks = setting;
    			break;
    			
    		case "exceptions":
    			Settings.exceptions = setting;
    			break;
    			
    		case "measures":
    			if (setting != Settings.measureChecks)
    			{
	    			sb.append("Specification must now be reloaded to take effect");
    			}

    			Settings.measureChecks = setting;
    			break;
    			
    		case "annotations":
    			if (setting != Settings.annotations)
    			{
	    			sb.append("Specification must now be reloaded to take effect");
    			}

    			Settings.annotations = setting;
    			break;
    			
    		default:
    			sb.append(USAGE);
    			break;
		}
		
		return new DAPMessageList(request, false, sb.toString(), null);
	}
	
	private void isEnabled(String name, boolean flag)
	{
		sb.append(name + (flag ? " are enabled" : " are disabled") + "\n");
	}
}
