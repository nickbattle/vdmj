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

package lsp.textdocument;

import java.io.File;
import java.net.URISyntaxException;

import json.JSONObject;
import lsp.LSPHandler;
import lsp.Utils;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.plugins.LSPPlugin;

public class ReferencesHandler extends LSPHandler
{
	public ReferencesHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			JSONObject textDocument = params.get("textDocument");
			File file = Utils.uriToFile(textDocument.get("uri"));
			
			JSONObject position = params.get("position");
			Long line = position.get("line");
			Long col = position.get("character");
			
			JSONObject context = params.get("context");
			Boolean incdec = context.get("includeDeclaration");
			
			return LSPPlugin.getInstance().lspReferences(request,
					file, line.intValue(), col.intValue(), incdec);
		}
		catch (URISyntaxException e)
		{
			Diag.error(e);
		}
		catch (Exception e)
		{
			Diag.error(e);
		}

		return null;
	}
}
