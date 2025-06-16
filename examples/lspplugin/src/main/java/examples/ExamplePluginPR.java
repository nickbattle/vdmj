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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package examples;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;

import dap.DAPMessageList;
import json.JSONArray;
import rpc.RPCMessageList;
import workspace.MessageHub;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckFailedEvent;
import workspace.events.CodeLensEvent;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;
import workspace.lenses.TCCodeLens;
import workspace.plugins.TCPlugin;

public class ExamplePluginPR extends ExamplePlugin
{
	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		System.out.println("ExamplePluginPR got " + event);

		if (event instanceof CodeLensEvent)
		{
			CodeLensEvent le = (CodeLensEvent)event;
			return new RPCMessageList(event.request, getCodeLenses(le.file));
		}
		else if (event instanceof CheckCompleteEvent)
		{
			CheckCompleteEvent ce = (CheckCompleteEvent)event;
			addFirstWarning(ce);
			return null;
		}
		else if (event instanceof CheckFailedEvent)
		{
			CheckFailedEvent cfe = (CheckFailedEvent)event;
			fixTCMessages(cfe);
			return null;
		}
		else
		{
			return null;
		}
	}

	@Override
	public DAPMessageList handleEvent(DAPEvent event) throws Exception
	{
		System.out.println("ExamplePluginPR got " + event);
		return null;
	}
	
	/**
	 * Go through the ClassList from the TC plugin and issue a warning for
	 * the first definition of the first module, as an example.
	 */
	private void addFirstWarning(CheckCompleteEvent event) throws IOException
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCClassList tcClassList = tc.getTC();
		
		for (TCDefinition def: tcClassList.get(0).definitions)
		{
			if (def.name != null)
			{
				VDMWarning warning = new VDMWarning(9999, "Example warning from plugin", def.name.getLocation());
				MessageHub.getInstance().addPluginMessage(this, warning);	// Add the warning to the hub
				break;
			}
		}
	}

	/**
	 * To apply code lenses for PP/RT, we get the TC plugin to obtain a ClassList of
	 * type-checked classes, and then search through them for TCDefinitions that are
	 * within the File that is passed from the Client (ie. the file on screen).
	 * 
	 * This way of splitting lenses into getCodeLenses and getTCCodeLenses is just
	 * a convention. The only requirement is that this method returns the lenses
	 * required.
	 */
	private JSONArray getCodeLenses(File file)
	{
		TCPlugin tc = registry.getPlugin("TC");
		TCClassList tcClassList = tc.getTC();
		JSONArray results = new JSONArray();
		
		if (!tcClassList.isEmpty())	// May be syntax errors
		{
			List<TCCodeLens> lenses = getTCCodeLenses();
			
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					for (TCDefinition def: clazz.definitions)	// ie. within this class/file
					{
						for (TCCodeLens lens: lenses)
						{
							results.addAll(lens.getDefinitionLenses(def, clazz));
						}
					}
				}
			}
		}
		
		return results;
	}
}
