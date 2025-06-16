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

package lsp.workspace;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPHandler;
import lsp.Utils;
import lsp.textdocument.WatchKind;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.plugins.LSPPlugin;

public class DidChangeWSHandler extends LSPHandler
{
	public DidChangeWSHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		switch (request.getMethod())
		{
			case "workspace/didChangeWatchedFiles":
				return didChangeWatchedFiles(request);
			
			default:
				return new RPCMessageList(request, RPCErrors.InternalError, "Unexpected workspace message");
		}
	}

	private RPCMessageList didChangeWatchedFiles(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			JSONArray changes = params.get("changes");
			int actionCode = 0;
			List<File> deleted = new Vector<File>();
			
			for (Object fileEvent: changes)
			{
				if (fileEvent instanceof JSONObject)
				{
					JSONObject change = (JSONObject)fileEvent;
					String uri = change.get("uri");
					
					if (uri.startsWith("file"))
					{
						WatchKind type = WatchKind.kindOf(change.get("type"));
						File file = Utils.uriToFile(uri);
						int code = LSPPlugin.getInstance().lspDidChangeWatchedFile(request, file, type);
						
						if (code > actionCode)	// Note: ordered severity
						{
							actionCode = code;
						}
						
						if (type == WatchKind.DELETE)
						{
							deleted.add(file);
						}
					}
					else
					{
						Diag.info("WARNING: ignoring non-file URI", uri);
					}
				}
			}
			
			// Do rebuilding and type checking after ALL the changes are processed
			// This can return null, since didChangeWatchedFiles is a notification.
			
			return LSPPlugin.getInstance().afterChangeWatchedFiles(request, actionCode, deleted);
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
}
