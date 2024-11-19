/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package workspace.lenses;

import com.fujitsu.vdmj.lex.LexLocation;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import workspace.Diag;
import workspace.plugins.LSPPlugin;

/**
 * The base class for all code lenses.
 */
abstract public class CodeLens
{
	/**
	 * Lenses are often dependent on particular LSP Clients that implement the
	 * commands returned. This method extracts the client name from the initialization
	 * echange with the Client.
	 */
	protected String getClientName()
	{
		return LSPPlugin.getInstance().getClientInfo("name");
	}
	
	/**
	 * LSP clients sometimes have different names for the same client. This
	 * method enables a general check, allowing multiple names.
	 */
	protected boolean isClientType(String type)
	{
		String client = getClientName();
		
		switch (type)
		{
			case "vscode":
				return client.equals("vscode") || client.equals("Visual Studio Code");
				
			default:
				Diag.error("Unknown client test: %s", type);
				return false;
		}
	}
	
	/**
	 * These helper methods generate the lens response body.
	 */
	protected JSONObject makeLens(LexLocation location, String title, String command)
	{
		return new JSONObject(
				"range", Utils.lexLocationToRange(location),
				"command", new JSONObject("title", title, "command", command));
	}
	
	protected JSONObject makeLens(LexLocation location, String title, String command, JSONArray arguments)
	{
		return new JSONObject(
				"range", Utils.lexLocationToRange(location),
				"command", new JSONObject("title", title, "command", command, "arguments", arguments));
	}
}
