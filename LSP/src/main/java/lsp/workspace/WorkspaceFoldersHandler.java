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

package lsp.workspace;

import java.io.File;
import java.util.List;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPHandler;
import lsp.LSPServerState;
import lsp.Utils;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import workspace.Log;

public class WorkspaceFoldersHandler extends LSPHandler
{
	public WorkspaceFoldersHandler(LSPServerState state)
	{
		super(state);
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		return null;	// Not used - see response megthod
	}
	
	@Override
	public void response(RPCResponse message)
	{
		if (message.isError())
		{
			Log.error("Error response received: %s", message.getError());
			return;
		}
		
		JSONArray result = message.get("result");
		List<File> roots = lspServerState.getManager().getRoots();
		
		try
		{
			for (int i=0; i<result.size(); i++)
			{
				JSONObject item = result.index(i);
				String uri = item.get("uri");
				File file = Utils.uriToFile(uri);
				
				if (roots.contains(file))
				{
					Log.printf("Roots contains %s", uri);
				}
				else
				{
					Log.printf("Roots does NOT contain %s", uri);
				}
			}
		}
		catch (Exception e)
		{
			Log.error(e);
		}
	}
}
