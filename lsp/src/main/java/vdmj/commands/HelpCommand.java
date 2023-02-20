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

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.PluginRegistry;

public class HelpCommand extends AnalysisCommand
{
	public static final String USAGE = "Usage: help [command]";
	public static final String HELP = "help [<command>] - information about commands";
	
	private String command = null;

	public HelpCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		if (parts.length == 2)
		{
			this.command = parts[1];
		}
		else if (parts.length != 1)
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		StringBuilder sb = new StringBuilder();
		HelpList help = PluginRegistry.getInstance().getCommandHelp();
		
		for (String cmd: help.keySet())
		{
			if (command == null || cmd.equals(command))
			{
				sb.append(help.get(cmd) + "\n");
			}
		}

		if (sb.length() == 0)
		{
			sb.append("Unknown command '" + command + "'");
		}
		
		return new DAPMessageList(request, new JSONObject("result", sb.toString()));
	}

	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
