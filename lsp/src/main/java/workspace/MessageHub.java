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

package workspace;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.plugins.AnalysisPlugin;

/**
 * A singleton object to manage the current set of error and warning messages for all
 * of the source files in the workspace. 
 */
public class MessageHub
{
	private static MessageHub INSTANCE = null;
	
	private final Map<File, Map<String, Set<VDMMessage>>> messageMap;
	private final Map<String, AnalysisPlugin> pluginMap;
	
	private MessageHub()
	{
		this.messageMap = new HashMap<File, Map<String, Set<VDMMessage>>>();
		this.pluginMap = PluginRegistry.getInstance().getPlugins();
		Diag.info("Created MessageHub");
	}
	
	public synchronized static MessageHub getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new MessageHub();
		}
		
		return INSTANCE;
	}
	
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE.messageMap.clear();
			INSTANCE = null;
		}
	}
	
	/**
	 * This is used by the workspace manager to populate a blank plugin map for a
	 * new file.
	 */
	public void addFile(File file)
	{
		if (!messageMap.containsKey(file))
		{
			Map<String, Set<VDMMessage>> pmap = new HashMap<String, Set<VDMMessage>>();
			
			for (String pname: pluginMap.keySet())
			{
				pmap.put(pname, new HashSet<VDMMessage>());
			}
			
			messageMap.put(file, pmap);
			Diag.info("MessageHub added file %s", file);
		}
		else
		{
			Diag.error("Cannot add MessageHub file %s", file);
		}
	}
	
	/**
	 * Add a list of messages for files (assumed to be already added).
	 */
	public void addPluginMessages(AnalysisPlugin plugin, List<VDMMessage> messages)
	{
		String pname = plugin.getName();
		
		for (VDMMessage message: messages)
		{
			Map<String, Set<VDMMessage>> pmap = messageMap.get(message.location.file);
			
			if (pmap != null)
			{
				Set<VDMMessage> pmessages = pmap.get(pname);
				
				if (pmessages != null)
				{
					pmessages.add(message);
				}
				else
				{
					Diag.error("Cannot add message for plugin name %s", pname);
				}
			}
			else
			{
				Diag.error("Cannot add message for file %s", message.location.file);
			}
		}
	}
	
	/**
	 * Clear all messages that relate to a given plugin.
	 */
	public void clearPluginMessages(AnalysisPlugin plugin)
	{
		String pname = plugin.getName();
		
		for (File file: messageMap.keySet())
		{
			Map<String, Set<VDMMessage>> pmap = messageMap.get(file);
			pmap.get(pname).clear();
		}
	}
	
	/**
	 * Clear all file messages. This is used when the project is reloaded
	 */
	public void clear()
	{
		messageMap.clear();
		Diag.info("MessageHub cleared");
	}
	
	/**
	 * Get a complete list of LSP diagnostic responses for the files passed.
	 */
	public RPCMessageList getDiagnosticResponses(List<File> filesToReport)
	{
		RPCMessageList responses = new RPCMessageList();

		for (File file: filesToReport)
		{
			Map<String, Set<VDMMessage>> pmap = messageMap.get(file);
			
			if (pmap != null)
			{
				JSONArray messages = new JSONArray();

				for (String pname: pmap.keySet())
				{
					for (VDMMessage message: pmap.get(pname))
					{
						messages.add(
							new JSONObject(
								"range",	Utils.lexLocationToRange(message.location),
								"severity", (message instanceof VDMError ? 1 : 2),
								"code", 	message.number,
								"message",	message.toProblemString().replaceAll("\n", ", ")));
						
					}
				}
				
				JSONObject params = new JSONObject("uri", file.toURI().toString(), "diagnostics", messages);
				responses.add(RPCRequest.notification("textDocument/publishDiagnostics", params));
			}
			else
			{
				Diag.error("Cannot get diagnostic messages for file %s", file);
			}
		}
		
		return responses;
	}
}
