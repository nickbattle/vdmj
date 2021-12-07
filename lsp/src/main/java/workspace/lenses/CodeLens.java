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

import java.io.File;

import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import workspace.LSPWorkspaceManager;

/**
 * The base class for all code lenses.
 */
abstract public class CodeLens
{
	/**
	 * Lenses can be generated from the AST while entering a specifications. They
	 * can also be refreshed later from the TC after the spec is checked. Both
	 * of these are required. 
	 */
	abstract public JSONArray codeLenses(ASTDefinition definition, File file);
	abstract public JSONArray codeLenses(TCDefinition definition, File file);
	
	/**
	 * Lenses are often dependent on particular LSP Clients that implement the
	 * commands returned. This method extracts the client name from the initialization
	 * echange with the Client.
	 */
	protected String getClientName()
	{
		return LSPWorkspaceManager.getInstance().getClientInfo("name");
	}
	
	/**
	 * These helper methods generate the lens response body. The JSONArray returned
	 * by codeLenses (above) is an array of these structures, one per lens. 
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
