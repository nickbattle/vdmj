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

package workspace.plugins;

import dap.DAPMessageList;
import json.JSONObject;
import lsp.LSPMessageUtils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import vdmj.commands.Command;
import vdmj.commands.HelpList;
import workspace.EventHub;
import workspace.PluginRegistry;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;

abstract public class AnalysisPlugin
{
	protected final LSPMessageUtils messages;
	protected final PluginRegistry registry;
	protected final EventHub eventhub;
	
	public AnalysisPlugin()
	{
		messages = new LSPMessageUtils();
		registry = PluginRegistry.getInstance();
		eventhub = EventHub.getInstance();
	}
	
	abstract public String getName();
	
	abstract public void init();
	
	/**
	 * These methods are used to dispatch LSP/DAP events. These default methods just return an
	 * error, usually indicating that an event has been registered with the EventHub, but
	 * no handler provided.
	 */
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		return new RPCMessageList(event.request, RPCErrors.InternalError, "Plugin does not handle LSP events");
	}

	public DAPMessageList handleEvent(DAPEvent event) throws Exception
	{
		return new DAPMessageList(event.request, false, "Plugin does not handle DAP events", null);
	}

	/**
	 * All plugins can register experimental options that are sent back to the Client
	 * in the experimental section of the initialize response. They can also set regular
	 * server capabilities, though this should be done with care!
	 */
	public void setServerCapabilities(JSONObject capabilities)
	{
		return;		// LSP capabilities
	}

	public void setDAPCapabilities(JSONObject capabilities)
	{
		return;		// DAP capabilities
	}

	/**
	 * Plugins can return Commands to execute in the console. They are passed
	 * the whole command line, so that they can process arguments.
	 */
	public Command getCommand(String line)
	{
		return null;
	}

	/**
	 * Returns an array of String arrays for Command help. The first string is the
	 * simple name of the command, the 2nd is the detail of the usage. 
	 */
	public HelpList getCommandHelp()
	{
		return new HelpList();
	}
}
