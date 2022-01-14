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
import workspace.PluginRegistry;
import workspace.plugins.AnalysisPlugin;
import workspace.plugins.TCPlugin;

/**
 * All LSP plugins must extend AnalysisPlugin. The fully qualified class name of the plugin must be set
 * in the "lspx.plugins" property, to make the LSP Server load it.
 */
public class V2CPlugin extends AnalysisPlugin
{
	/**
	 * A plugin must provide a default constructor, as here or a static "factory" method that takes
	 * a single Dialect parameter.
	 * 
	 * public static V2CPlugin factory(Dialect dialect)
	 */
	public V2CPlugin()
	{
		super();
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
		// Ignore
	}
	
	/**
	 * A plugin can support a number of LSP methods, but here we match just one.
	 */
	@Override
	public boolean supportsMethod(String method)
	{
		return method.equals("slsp/v2c");
	}
	
	/**
	 * The analyse method is called when the LSP client sends a request that has a name
	 * recognised by the supportsMethod method above. It is passed the JSON request and
	 * returns a list of JSON responses.
	 */
	@Override
	public RPCMessageList analyse(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			File saveUri = Utils.uriToFile(params.get("saveUri"));

			TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
			Object tclist = tc.getTC();
			
			if (tclist == null)
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification is not checked");
			}
			
			File output = new File(saveUri, "output.c");
			PrintWriter outstream = new PrintWriter(output);
			
			if (tclist instanceof TCModuleList)
			{
				TCModuleList mlist = (TCModuleList) tclist;
				TRModuleList trModules = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(mlist);
				outstream.println(trModules.translate());
				outstream.close();
			}
			else if (tclist instanceof TCClassList)
			{
				TCClassList clist = (TCClassList) tclist;
				TRClassList trClasses = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(clist);
				outstream.println(trClasses.translate());
				outstream.close();
			}
			else
			{
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