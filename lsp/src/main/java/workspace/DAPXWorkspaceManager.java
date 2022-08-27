/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import lsp.LSPException;
import lsp.Utils;
import rpc.RPCErrors;
import workspace.events.UnknownCommandEvent;
import workspace.plugins.CTPlugin;

public class DAPXWorkspaceManager
{
	private static DAPXWorkspaceManager INSTANCE = null;
	private final PluginRegistry registry;
	private final EventHub eventhub;
	private final DAPWorkspaceManager dapManager;
	
	protected DAPXWorkspaceManager()
	{
		this.registry = PluginRegistry.getInstance();
		this.eventhub = EventHub.getInstance();
		this.dapManager = DAPWorkspaceManager.getInstance();
	}

	public static synchronized DAPXWorkspaceManager getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new DAPXWorkspaceManager();		
			Diag.info("Created DAPXWorkspaceManager");
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
			INSTANCE = null;
		}
	}
	
	/**
	 * DAPX extensions...
	 */
	public JSONObject ctRunOneTrace(DAPRequest request, String name, long testNumber) throws LSPException
	{
		CTPlugin ct = registry.getPlugin("CT");
		
		if (ct.isRunning())
		{
			Diag.error("Previous trace is still running...");
			throw new LSPException(RPCErrors.InvalidRequest, "Trace still running");
		}

		/**
		 * If the specification has been modified since we last ran (or nothing has yet run),
		 * we have to re-create the interpreter, otherwise the old interpreter (with the old tree)
		 * is used to "generate" the trace names, so changes are not picked up. Note that a
		 * new tree will have no breakpoints, so if you had any set via a launch, they will be
		 * ignored.
		 */
		dapManager.refreshInterpreter();
		
		if (dapManager.specHasErrors())
		{
			throw new LSPException(RPCErrors.ContentModified, "Specification has errors");
		}
		
		dapManager.setNoDebug(false);	// Force debug on for runOneTrace

		return ct.runOneTrace(Utils.stringToName(name), testNumber);
	}

	public DAPMessageList unhandledCommand(DAPRequest request)
	{
		DAPMessageList responses = eventhub.publish(new UnknownCommandEvent(request));
		
		if (responses.isEmpty())
		{
			Diag.error("No external plugin registered for unknownMethodEvent (%s)", request.getCommand());
			return new DAPMessageList(request, false, "Unknown command: " + request.getCommand(), null);
		}
		else
		{
			return responses;
		}
	}
}
