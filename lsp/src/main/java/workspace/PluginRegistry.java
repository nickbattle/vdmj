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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import json.JSONArray;
import json.JSONObject;
import vdmj.commands.Command;
import vdmj.commands.ErrorCommand;
import vdmj.commands.HelpList;
import workspace.plugins.AnalysisPlugin;

public class PluginRegistry
{
	private static PluginRegistry INSTANCE = null;
	private final Map<String, AnalysisPlugin> plugins;

	private PluginRegistry()
	{
		plugins = new HashMap<String, AnalysisPlugin>();
	}

	public static synchronized PluginRegistry getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new PluginRegistry();
		}
		
		return INSTANCE;
	}
	
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE.plugins.clear();
			INSTANCE = null;
		}
	}
	
	public void registerPlugin(AnalysisPlugin plugin)
	{
		plugins.put(plugin.getName(), plugin);
		plugin.init();
		Diag.config("Registered analysis plugin: %s", plugin.getName());
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPlugin(String name)
	{
		return (T)plugins.get(name);
	}
	
	public void setPluginCapabilities(JSONObject capabilities)
	{
		for (AnalysisPlugin plugin: plugins.values())
		{
			try
			{
				plugin.setServerCapabilities(capabilities);
			}
			catch (Throwable e)
			{
				Diag.error("Exception in %s setServerCapabilities", plugin.getName());
				Diag.error(e);
			}
		}
	}
	
	public JSONArray getCodeLenses(File file)
	{
		JSONArray commands = new JSONArray();
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			try
			{
				commands.addAll(plugin.getCodeLenses(file));
			}
			catch (Throwable e)
			{
				Diag.error("Exception in %s getCodeLenses", plugin.getName());
				Diag.error(e);
			}
		}
		
		return commands;
	}
	
	public Command getCommand(String line)
	{
		Command result = null;
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			try
			{
				Command c = plugin.getCommand(line);
				
				if (c != null)
				{
					if (result != null)
					{
						Diag.error("Multiple plugins support %s", line);
					}
					
					result = c;
				}
			}
			catch (IllegalArgumentException e)	// Usage failed
			{
				Diag.error(e.getMessage());
				return new ErrorCommand(e.getMessage()); 
			}
			catch (Throwable e)
			{
				Diag.error("Exception in %s getCommand", plugin.getName());
				Diag.error(e);
			}
		}
		
		return result;
	}
	
	public HelpList getCommandHelp()
	{
		HelpList result = new HelpList();
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			try
			{
				HelpList messages = plugin.getCommandHelp();
				result.add(messages);
			}
			catch (Throwable e)
			{
				Diag.error("Exception in %s getCommandHelp", plugin.getName());
				Diag.error(e);
			}
		}
		
		return result;
	}
}
