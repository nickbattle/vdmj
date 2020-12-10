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
 *
 ******************************************************************************/

package lsp.lspx;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import json.JSONObject;
import lsp.LSPHandler;
import lsp.LSPServerState;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.LSPXWorkspaceManager;
import workspace.Log;

public class TranslateHandler extends LSPHandler
{
	public TranslateHandler(LSPServerState state)
	{
		super(state);
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
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
					return LSPXWorkspaceManager.getInstance().translateLaTeX(request, file, saveUri);
				
				case "word":
					return LSPXWorkspaceManager.getInstance().translateWord(request, file, saveUri);
				
				default:
					return new RPCMessageList(request, RPCErrors.InvalidParams, "Unsupported language");
			}
			
		}
		catch (URISyntaxException e)
		{
			return new RPCMessageList(request, RPCErrors.InvalidParams, "URI syntax error");
		}
		catch (IOException e)
		{
			Log.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
