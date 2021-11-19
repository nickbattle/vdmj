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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import json.JSONObject;
import workspace.lenses.CodeLens;
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
	
	/**
	 * This is only used by unit testing.
	 */
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
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPlugin(String name)
	{
		return (T)plugins.get(name);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getPluginForMethod(String method)
	{
		T result = null;
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			if (plugin.supportsMethod(method))
			{
				if (result != null)
				{
					Log.error("Multiple plugins support %s", method);
				}
				
				result = (T)plugin;
			}
		}
		
		return result;
	}

	public JSONObject getExperimentalOptions()
	{
		JSONObject options = new JSONObject();
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			options.putAll(plugin.getExperimentalOptions());
		}
		
		return options;
	}
	
	public List<CodeLens> getCodeLenses()
	{
		List<CodeLens> options = new Vector<CodeLens>();
		
		for (AnalysisPlugin plugin: plugins.values())
		{
			options.addAll(plugin.getCodeLenses());
		}
		
		return options;
	}
}
