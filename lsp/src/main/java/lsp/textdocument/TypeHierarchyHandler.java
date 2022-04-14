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

package lsp.textdocument;

import java.io.File;
import java.net.URISyntaxException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import json.JSONObject;
import lsp.LSPHandler;
import lsp.Utils;
import rpc.RPCErrors;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.LSPWorkspaceManager;

public class TypeHierarchyHandler extends LSPHandler
{
	public TypeHierarchyHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		if (Settings.dialect == Dialect.VDM_SL)
		{
			return new RPCMessageList(request, null);
		}
		
		switch (request.getMethod())
		{
			case "textDocument/prepareTypeHierarchy":
				return prepareTypeHierarchy(request);
			
			case "typeHierarchy/supertypes":
				return getHierarchy(request, false);
			
			case "typeHierarchy/subtypes":
				return getHierarchy(request, true);

			default:
				return new RPCMessageList(request, RPCErrors.MethodNotFound, "Unexpected type hierarchy method");
		}
	}

	private RPCMessageList prepareTypeHierarchy(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			JSONObject textDocument = params.get("textDocument");
			File file = Utils.uriToFile(textDocument.get("uri"));
			
			JSONObject position = params.get("position");
			Long line = position.get("line");
			Long col = position.get("character");
			
			return LSPWorkspaceManager.getInstance().prepareHierarchy(request,
					file, line.intValue(), col.intValue());
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

	private RPCMessageList getHierarchy(RPCRequest request, boolean subtypes)
	{
		try
		{
			JSONObject params = request.get("params");
			JSONObject item = params.get("item");
			String classname = item.get("name");
			
			return LSPWorkspaceManager.getInstance().getTypeHierarchy(request, classname, subtypes);
		}
		catch (Exception e)
		{
			Diag.error(e);
		}

		return null;
	}
}
