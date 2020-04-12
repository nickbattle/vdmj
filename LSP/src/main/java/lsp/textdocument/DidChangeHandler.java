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

package lsp.textdocument;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import json.JSONArray;
import json.JSONObject;
import lsp.LSPHandler;
import lsp.LSPServerState;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class DidChangeHandler extends LSPHandler
{
	public DidChangeHandler(LSPServerState state)
	{
		super(state);
	}

	@Override
	public RPCMessageList run(RPCRequest request) throws IOException
	{
		try
		{
			JSONObject params = request.get("params");
			JSONObject textDocument = params.get("textDocument");
			File file = Utils.uriToFile(textDocument.get("uri"));
			
			JSONArray contentChanges = params.get("contentChanges");
			RPCMessageList result = new RPCMessageList();
			
			for (Object contentChange: contentChanges)
			{
				if (contentChange instanceof JSONObject)
				{
					JSONObject change = (JSONObject)contentChange;
					JSONObject range = change.get("range");
					String text = change.get("text");
					RPCMessageList r = lspServerState.getManager().changeFile(request, file, range, text);
					if (r != null) result.addAll(r);
				}
			}
			
			return result;
		}
		catch (URISyntaxException e)
		{
			return new RPCMessageList(request, "URI syntax error");
		}
		catch (Exception e)
		{
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
