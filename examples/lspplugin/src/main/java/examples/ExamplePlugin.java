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

import com.fujitsu.vdmj.lex.Dialect;

import dap.DAPMessageList;
import json.JSONArray;
import json.JSONObject;
import rpc.RPCMessageList;
import vdmj.commands.Command;
import vdmj.commands.HelpList;
import workspace.Diag;
import workspace.EventHub;
import workspace.EventListener;
import workspace.events.ChangeFileEvent;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckSyntaxEvent;
import workspace.events.CheckTypeEvent;
import workspace.events.CloseFileEvent;
import workspace.events.DAPBeforeEvaluateEvent;
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
import workspace.events.UnknownTranslationEvent;
import workspace.lenses.TCCodeLens;
import workspace.plugins.AnalysisPlugin;

abstract public class ExamplePlugin extends AnalysisPlugin implements EventListener
{
	/**
	 * Most plugins will have at least two subclasses that deal with modules and classes,
	 * respectively. This factory method is called, if it exists, when the lspx.plugins
	 * list is being processed. Otherwise a dialect-specific plugin class can be given
	 * in lspx.plugins.
	 */
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
				Diag.error("Unsupported dialect " + dialect);
				throw new IllegalArgumentException("Unsupported dialect: " + dialect);
		}
	}

	/**
	 * Most plugins will have SL, PP and RT subclasses, since they have to deal with
	 * modules and classes differently. In that case, it is easiest to implement a
	 * factory(dialect) method, as above. But if there was a single-dialect plugin,
	 * it could create a public constructor here instead.
	 */
	protected ExamplePlugin()
	{
		// Not used, because of the factory method above.
	}
	
	/**
	 * The init method is called when the plugin is registered with the PluginRegistry.
	 * Typically, it registers itself with the EventHub to receive various events, for
	 * which the class has to implement EventListener.
	 */
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
		eventhub.register(UnknownTranslationEvent.class, this);
		eventhub.register(ShutdownEvent.class, this);

		eventhub.register(DAPInitializeEvent.class, this);
		eventhub.register(DAPBeforeEvaluateEvent.class, this);
		eventhub.register(DAPLaunchEvent.class, this);
		eventhub.register(DAPConfigDoneEvent.class, this);
		eventhub.register(DAPEvaluateEvent.class, this);
		eventhub.register(DAPDisconnectEvent.class, this);
		eventhub.register(DAPTerminateEvent.class, this);

		eventhub.register(UnknownMethodEvent.class, this);
		eventhub.register(UnknownCommandEvent.class, this);
	}
	
	/**
	 * At LSP initialization, a plugin can set or change any LSPServer capability
	 * responses that it wants to, by implementing this method.
	 */
	@Override
	public void setServerCapabilities(JSONObject capabilities)
	{
		// Just an example. See initialize response.
		JSONObject experimental = capabilities.get("experimental");
		experimental.put("exampleProvider", true);	
	}
	
	/**
	 * This is just one way of implementing code lenses. This method is used by the
	 * getCodeLenses methods in the dialect subclasses, and is intended to return
	 * a list of the code lenses that this plugin provides. These could be cached,
	 * unless the code lenses themselves contain state.
	 */
	protected List<TCCodeLens> getTCCodeLenses()
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
	abstract public JSONArray getCodeLenses(File file);
	
	/**
	 * This method is called when the user types a line that is not recognised by the
	 * built-in commands. It is responsible for returning an instance of the Command
	 * provided by the plugin, if the line matches. A plugin can offer many commands.
	 * Typically, the first word on the line indicates the Command to use, but this is
	 * only a convention.
	 */
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
	
	/**
	 * This method returns the HelpList of help lines for all the Commands
	 * recognised above. By convention, each Command has a field called HELP that
	 * contains a helpful message. For example:
	 * 
	 * String[] HELP = "example <text> - echo text to the console";
	 */
	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList
		(
			ExampleCommand.HELP
			// Other commands' help lines here...
		);
	}

	/**
	 * The name of the plugin allows other plugins to obtain the instance via the registry.
	 * For example, registry.getPlugin("Example"). This is a generic method, so it expects
	 * the type to match the variable being assigned.
	 */
	@Override
	public String getName()
	{
		return "Example";
	}

	/**
	 * These two events are implemented in the subclasses, and react to events received.
	 * In the example, they just print out what they received, but in general the plugin
	 * would react in some way, and return messages to send to the Client.
	 */
	@Override
	abstract public RPCMessageList handleEvent(LSPEvent event) throws Exception;
	
	@Override
	abstract public DAPMessageList handleEvent(DAPEvent event) throws Exception;
}