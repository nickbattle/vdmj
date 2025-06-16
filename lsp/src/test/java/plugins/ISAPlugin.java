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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package plugins;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONArray;
import json.JSONObject;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.EventHub;
import workspace.EventListener;
import workspace.events.LSPEvent;
import workspace.events.UnknownTranslationEvent;
import workspace.plugins.AnalysisPlugin;

public abstract class ISAPlugin extends AnalysisPlugin implements EventListener
{
	public static ISAPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new ISAPluginSL();
				
			default:
				Diag.error("Unsupported dialect " + dialect);
				throw new IllegalArgumentException("Unsupported dialect: " + dialect);
		}
	}

	protected ISAPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "ISA";
	}

	@Override
	public void init()
	{
		EventHub.getInstance().register(UnknownTranslationEvent.class, this);
	}
	
	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		if (event instanceof UnknownTranslationEvent)
		{
			UnknownTranslationEvent ute = (UnknownTranslationEvent)event;
			
			if (ute.languageId.equals("isabelle"))
			{
				return analyse(event.request);
			}
		}
		
		return null;	// Not handled
	}
	
	abstract public RPCMessageList analyse(RPCRequest request);

	@Override
	public void setServerCapabilities(JSONObject capabilities)
	{
		JSONObject experimental = capabilities.get("experimental");
		
		if (experimental != null)
		{
			JSONObject provider = experimental.get("translateProvider");
			
			if (provider != null)
			{
				JSONArray ids = provider.get("languageId");
				
				if (ids != null)
				{
					ids.add("isabelle");	// Edit the standard response to include isabelle
				}
			}
		}
	}
}