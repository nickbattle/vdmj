/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package plugins;

import java.io.File;
import java.io.PrintWriter;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import examples.v2c.tr.TRNode;
import examples.v2c.tr.definitions.TRClassList;
import examples.v2c.tr.modules.TRModuleList;
import json.JSONObject;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.EventHub;
import workspace.EventListener;
import workspace.PluginRegistry;
import workspace.events.LSPEvent;
import workspace.events.UnknownMethodEvent;
import workspace.plugins.AnalysisPlugin;
import workspace.plugins.TCPlugin;

/**
 * All LSP plugins must extend AnalysisPlugin. The fully qualified class name of the plugin must be set
 * in the "lspx.plugins" property or resource file, to make the LSP Server load it.
 */
public class V2CPluginLSP extends AnalysisPlugin implements EventListener
{
	/**
	 * A plugin must provide a static "factory" method that takes a single Dialect parameter.
	 */
	public static AnalysisPlugin factory(Dialect dialect)
	{
		return new V2CPluginLSP();		// For all dialects. This could be specialized.
	}
	
	/**
	 * This is the registered name in the plugin registry. It can be anything.
	 */
	@Override
	public String getName()
	{
		return "V2C";
	}

	/**
	 * This method is called when the plugin is registered.
	 */
	@Override
	public void init()
	{
		EventHub.getInstance().register(UnknownMethodEvent.class, this);
	}
	
	/**
	 * This method is called when unknownMethodEvent, slsp/v2c events are raised.
	 * They go via the unknownMethod handler, which sends an unknownMethodEvent.
	 */
	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		if (event instanceof UnknownMethodEvent &&
			event.request.getMethod().equals("slsp/v2c"))
		{
			return analyse(event.request);
		}
		else
		{
			return null;	// Not handled
		}
	}

	/**
	 * The analyse method is called when the LSP client sends a request that has a name
	 * recognised by the handleEvent method above. It is passed the JSON request and
	 * returns a list of JSON responses.
	 */
	private RPCMessageList analyse(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			File saveUri = Utils.uriToFile(params.get("saveUri"));

			TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
			File output = new File(saveUri, "output.c");
			PrintWriter outstream = new PrintWriter(output);
			
			switch (Settings.dialect)
			{
				case VDM_SL:
					TCModuleList mlist = tc.getTC();
					TRModuleList trModules = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(mlist);
					outstream.println(trModules.translate());
					outstream.close();
					break;
					
				case VDM_PP:
				case VDM_RT:
					TCClassList clist = tc.getTC();
					TRClassList trClasses = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(clist);
					outstream.println(trClasses.translate());
					outstream.close();
					break;
					
				default:
					outstream.close();
					return new RPCMessageList(request, RPCErrors.InvalidRequest, "Unknown dialect?");
			}

			return new RPCMessageList(request, new JSONObject("uri", saveUri.toURI().toString()));
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}