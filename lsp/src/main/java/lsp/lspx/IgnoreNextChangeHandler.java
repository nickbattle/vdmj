/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package lsp.lspx;

import rpc.RPCRequest;
import workspace.Diag;
import workspace.plugins.LSPPlugin;

import json.JSONObject;
import lsp.LSPHandler;
import rpc.RPCErrors;
import rpc.RPCMessageList;

/**
 * This is a non-standard notification. We use it to allow a subsequent didChange
 * request to be ignored. This is so that it can be used to provoke a refresh of
 * the outline view after a save, without causing the AST buffer to become dirty.
 */
public class IgnoreNextChangeHandler extends LSPHandler
{
	public IgnoreNextChangeHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		try
		{
			JSONObject params = request.get("params");
			JSONObject range = params.get("range");
			LSPPlugin.getInstance().lspIgnoreNextChange(range);
			return null;	// notification
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new RPCMessageList(request, RPCErrors.InternalError, e.getMessage());
		}
	}
}
