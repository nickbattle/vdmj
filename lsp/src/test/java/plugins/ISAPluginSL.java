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

import java.io.File;
import java.io.PrintWriter;

import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import json.JSONObject;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.PluginRegistry;
import workspace.plugins.TCPlugin;

public class ISAPluginSL extends ISAPlugin
{
	public ISAPluginSL()
	{
		super();
	}
	
	@Override
	public RPCMessageList analyse(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			File saveUri = Utils.uriToFile(params.get("saveUri"));

			TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
			TCModuleList tclist = tc.getTC();
			
			if (tclist == null || tclist.isEmpty())
			{
				return new RPCMessageList(request, RPCErrors.InvalidRequest, "Specification is not checked");
			}
			
			for (TCModule module: tclist)
			{
				File outfile = new File(saveUri, module.name.getName() + ".thy");
				PrintWriter out = new PrintWriter(outfile);
				out.write("Isabelle output...\n");
				out.close();
			}
			
//			TRModuleList trModules = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(tclist);
//			
//			for (TRModule module: trModules)
//			{
//				File outfile = new File(saveUri, module.name.getName() + ".thy");
//				PrintWriter out = new PrintWriter(outfile);
//				out.write(module.translate());
//				out.close();
//			}

			return new RPCMessageList(request, new JSONObject("uri", saveUri.toURI().toString()));
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
