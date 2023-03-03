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

package vdmj.commands;

import java.util.Map.Entry;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import workspace.PluginRegistry;
import workspace.plugins.AnalysisPlugin;

public class PluginsCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: plugins";
	public static final String HELP = "plugins - list the loaded plugins";

	public PluginsCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("plugins"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		if (argv.length != 1)
		{
			return new DAPMessageList(request, false, USAGE, null);			
		}
		
		StringBuilder sb = new StringBuilder();
		
		for (Entry<String, AnalysisPlugin> plugin: PluginRegistry.getInstance().getPlugins().entrySet())
		{
			sb.append(plugin.getKey());
			sb.append(": ");
			sb.append(plugin.getValue().getDescription());
			sb.append("\n");
		}
		
		return new DAPMessageList(request, new JSONObject("result", sb.toString()));
	}

	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
