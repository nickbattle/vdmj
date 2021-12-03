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
import java.io.IOException;
import java.net.URISyntaxException;

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

public class TranslateHandler extends LSPHandler
{
	public TranslateHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		if (!LSPWorkspaceManager.getInstance().hasClientCapability("experimental.translateProvider"))
		{
			return new RPCMessageList(request, RPCErrors.MethodNotFound, "Translate capability is not enabled by client");
		}

		switch (request.getMethod())
		{
			case "slsp/TR/translate":
				return translate(request);

			default:
				return new RPCMessageList(request, RPCErrors.MethodNotFound, "Unexpected slsp/translate method");
		}
	}

	private RPCMessageList translate(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			File file = Utils.uriToFile(params.get("uri"));
			File saveUri = Utils.uriToFile(params.get("saveUri"));
			String language = params.get("languageId");
			JSONArray args = params.get("arguments");
			
			if (saveUri.exists())
			{
				if (saveUri.isDirectory())
				{
					if (saveUri.list().length != 0)
					{
						return new RPCMessageList(request, RPCErrors.InvalidParams, "saveUri is not empty");		
					}
				}
				else
				{
					return new RPCMessageList(request, RPCErrors.InvalidParams, "saveUri is not a folder");
				}
			}
			else
			{
				return new RPCMessageList(request, RPCErrors.InvalidParams, "saveUri does not exist");
			}
			
			switch (language)
			{
				case "latex":
				{
					boolean modelOnly = false;
					boolean markCoverage = false;
					boolean insertCoverageTables = false;
					
					// Search the array, looking for the three arguments possible, ignoring others
					if (args != null)
					{
						for (int i=0; i<args.size(); i++)
						{
							JSONObject arg = args.index(i);
							
							if (arg.containsKey("modelOnly"))
							{
								modelOnly = arg.get("modelOnly");
							}
							else if (arg.containsKey("markCoverage"))
							{
								markCoverage = arg.get("markCoverage");
							}
							else if (arg.containsKey("insertCoverageTables"))
							{
								insertCoverageTables = arg.get("insertCoverageTables");
							}
						}
					}

					return LSPXWorkspaceManager.getInstance().translateLaTeX(request, file, saveUri,
							modelOnly, markCoverage, insertCoverageTables);
				}
				
				case "word":
					return LSPXWorkspaceManager.getInstance().translateWord(request, file, saveUri);
				
				case "coverage":
					return LSPXWorkspaceManager.getInstance().translateCoverage(request, file, saveUri);
				
				case "graphviz":
					return LSPXWorkspaceManager.getInstance().translateGraphviz(request, file, saveUri);
				
				case "isabelle":
					return LSPXWorkspaceManager.getInstance().translateIsabelle(request);
				
				default:
					return new RPCMessageList(request, RPCErrors.InvalidParams, "Unsupported language");
			}
			
		}
		catch (URISyntaxException e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InvalidParams, "URI syntax error");
		}
		catch (IOException e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
