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

package lsp;

import rpc.RPCRequest;
import rpc.RPCResponse;
import workspace.Diag;
import workspace.LSPWorkspaceManager;
import java.io.File;
import java.net.URISyntaxException;

import json.JSONObject;
import rpc.RPCErrors;
import rpc.RPCMessageList;

public class InitializeHandler extends LSPHandler
{
	public InitializeHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		switch (request.getMethod())
		{
			case "initialize":
				return initialize(request);

			case "initialized":
				return initialized(request);
		
			default:
				return new RPCMessageList(request, RPCErrors.InternalError, "Unexpected initialize message");
		}
	}
	
	private RPCMessageList initialize(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			JSONObject clientInfo = params.get("clientInfo");
			File rootUri = Utils.uriToFile(params.get("rootUri"));
			JSONObject clientCapabilities = params.get("capabilities");
	
			return LSPWorkspaceManager.getInstance().lspInitialize(request, clientInfo, rootUri, clientCapabilities);
		}
		catch (URISyntaxException e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InvalidParams, "URI syntax error");
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}

	private RPCMessageList initialized(RPCRequest request)
	{
		LSPServer.getInstance().setInitialized(true);
		return LSPWorkspaceManager.getInstance().lspInitialized(request);
	}

	@Override
	public void response(RPCResponse message)
	{
		// Response to dynamic registrations
		Diag.info("Response to id %d received", (Long)message.get("id"));
	}
}
