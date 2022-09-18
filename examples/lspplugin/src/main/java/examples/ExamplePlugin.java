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

package examples;

import java.io.File;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import dap.DAPMessageList;
import json.JSONArray;
import json.JSONObject;
import rpc.RPCMessageList;
import vdmj.commands.Command;
import workspace.Diag;
import workspace.EventHub;
import workspace.EventListener;
import workspace.events.ChangeFileEvent;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckSyntaxEvent;
import workspace.events.CheckTypeEvent;
import workspace.events.CloseFileEvent;
import workspace.events.DAPConfigDoneEvent;
import workspace.events.DAPDisconnectEvent;
import workspace.events.DAPEvaluateEvent;
import workspace.events.DAPEvent;
import workspace.events.DAPInitializeEvent;
import workspace.events.DAPLaunchEvent;
import workspace.events.DAPTerminateEvent;
import workspace.events.InitializeEvent;
import workspace.events.InitializedEvent;
import workspace.events.LSPEvent;
import workspace.events.OpenFileEvent;
import workspace.events.SaveFileEvent;
import workspace.events.ShutdownEvent;
import workspace.events.UnknownCommandEvent;
import workspace.events.UnknownMethodEvent;
import workspace.lenses.TCCodeLens;
import workspace.plugins.AnalysisPlugin;

abstract public class ExamplePlugin extends AnalysisPlugin implements EventListener
{
	public static ExamplePlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new ExamplePluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new ExamplePluginPR();
				
			default:
				Diag.error("Unknown dialect " + dialect);
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
	}

	public ExamplePlugin()
	{
		// Not used, because of the factory method above.
	}
	
	@Override
	public void init()
	{
		EventHub eventhub = EventHub.getInstance();
		eventhub.register(InitializeEvent.class, this);
		eventhub.register(InitializedEvent.class, this);
		eventhub.register(OpenFileEvent.class, this);
		eventhub.register(ChangeFileEvent.class, this);
		eventhub.register(CloseFileEvent.class, this);
		eventhub.register(SaveFileEvent.class, this);
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckSyntaxEvent.class, this);
		eventhub.register(CheckTypeEvent.class, this);
		eventhub.register(CheckCompleteEvent.class, this);
		eventhub.register(UnknownMethodEvent.class, this);
		eventhub.register(ShutdownEvent.class, this);

		eventhub.register(DAPInitializeEvent.class, this);
		eventhub.register(DAPLaunchEvent.class, this);
		eventhub.register(DAPConfigDoneEvent.class, this);
		eventhub.register(DAPEvaluateEvent.class, this);
		eventhub.register(DAPDisconnectEvent.class, this);
		eventhub.register(DAPTerminateEvent.class, this);

		eventhub.register(UnknownCommandEvent.class, this);
	}
	
	@Override
	public void setServerCapabilities(JSONObject capabilities)
	{
		// Just an example. See initialize response.
		JSONObject experimental = capabilities.get("experimental");
		experimental.put("exampleProvider", true);	
	}
	
	protected List<TCCodeLens> getCodeLenses()
	{
		List<TCCodeLens> lenses = new Vector<TCCodeLens>();
		lenses.add(new ExampleLens());
		return lenses;
	}
	
	/**
	 * Since we provide a code lens (above), we should implement applyCodeLenses in
	 * both dialect subclasses.
	 */
	@Override
	abstract public JSONArray applyCodeLenses(File file);
	
	@Override
	public Command getCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		switch (parts[0])
		{
			case "example":
				return new ExampleCommand(line);
				
			// Other commands here...
				
			default:
				return null;
		}
	}
	
	@Override
	public String[][] getCommandHelp()
	{
		return new String[][]
		{
			ExampleCommand.HELP
			// Other commands' help lines here...
		};
	}

	@Override
	abstract public String getName();

	@Override
	abstract public RPCMessageList handleEvent(LSPEvent event) throws Exception;
	
	@Override
	abstract public DAPMessageList handleEvent(DAPEvent event) throws Exception;
}