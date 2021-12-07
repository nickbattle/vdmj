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

package lsp.lspx;

import java.io.File;
import java.net.URISyntaxException;

import com.fujitsu.vdmj.traces.TraceReductionType;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPHandler;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.LSPWorkspaceManager;
import workspace.LSPXWorkspaceManager;
import workspace.Log;

public class CTHandler extends LSPHandler
{
	public CTHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		if (!LSPWorkspaceManager.getInstance().hasClientCapability("experimental.combinatorialTesting"))
		{
			return new RPCMessageList(request, RPCErrors.MethodNotFound, "CT plugin is not enabled by client");
		}

		switch (request.getMethod())
		{
			case "slsp/CT/traces":
				return traces(request);

			case "slsp/CT/generate":
				return generate(request);

			case "slsp/CT/execute":
				return execute(request);

			default:
				return new RPCMessageList(request, RPCErrors.MethodNotFound, "Unexpected slsp/CT method");
		}
	}

	private RPCMessageList traces(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			File project = params == null ? null : Utils.uriToFile(params.get("uri"));
			return LSPXWorkspaceManager.getInstance().ctTraces(request, project);
		}
		catch (URISyntaxException e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InvalidParams, "URI syntax error");
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	private RPCMessageList generate(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			String name = params.get("name");
			return LSPXWorkspaceManager.getInstance().ctGenerate(request, name);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	private RPCMessageList execute(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			String tracename = params.get("name");
			JSONArray filter = params.get("filter");
			JSONObject range = params.get("range");
			Object partialResultToken = params.get("partialResultToken");
			Object workDoneToken = params.get("workDoneToken");
			
			TraceReductionType rType = TraceReductionType.NONE;
			float subset = 1.0F;
			long seed = 0;

			if (filter != null)
			{
				for (int i=0; i<filter.size(); i++)
				{
					JSONObject option = filter.index(i);
					String key = option.get("key");
					Object value = option.get("value");
					
					switch (key)
					{
						case "trace reduction type":
							rType = TraceReductionType.valueOf((String)value);
							break;
							
						case "subset limitation":
							subset = ((Long)value).floatValue()/100;
							break;
							
						case "trace filtering seed":
							seed = (Long)value;
							break;
							
						default:
							return new RPCMessageList(request, RPCErrors.InternalError, "Unknown key: " + key);
					}
				}
			}
			
			Long start = null;
			Long end = null;
			
			if (range != null)
			{
				start = range.get("start");
				end = range.get("end");
			}
			
			return LSPXWorkspaceManager.getInstance().ctExecute(request, tracename,
					partialResultToken, workDoneToken, rType, subset, seed, start, end);
		}
		catch (Exception e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
