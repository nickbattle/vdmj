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

import dap.DAPServerSocket;
import json.JSONArray;
import json.JSONObject;
import workspace.PluginRegistry;
import workspace.plugins.LSPPlugin;

public class LSPInitializeResponse extends JSONObject
{
	private static final long serialVersionUID = 1L;
	
	public LSPInitializeResponse()
	{
		String version = com.fujitsu.vdmj.util.Utils.getVersion();
		if (version == null) version = "unknown";
		put("serverInfo", new JSONObject("name", "VDMJ LSP Server", "version", version));
		put("capabilities", getServerCapabilities());
	}

	private JSONObject getServerCapabilities()
	{
		JSONObject cap = new JSONObject();
		LSPPlugin manager = LSPPlugin.getInstance();
		
		
		cap.put("definitionProvider", true);			// Go to definition for F12
		cap.put("documentSymbolProvider", true);		// Symbol information for Outline view

		cap.put("completionProvider",					// Completions
			new JSONObject(
				"triggerCharacters", new JSONArray(".", "`"),
				"resolveProvider", false));
		
		cap.put("textDocumentSync",
			new JSONObject(
				"openClose", true,
				"save", !manager.hasClientCapability("workspace.didChangeWatchedFiles.dynamicRegistration"),
				"change", 2				// incremental
			));
		
		cap.put("codeLensProvider",
			new JSONObject("resolveProvider", false));
		
		cap.put("referencesProvider", true);
		
		cap.put("typeHierarchyProvider", true);
		
		/**
		 * Experimental responses are partly fixed, from the implicit Server functions, and
		 * party added by registered plugins.
		 */
		cap.put("experimental",
				new JSONObject(
						"translateProvider", new JSONObject(
								"languageId", new JSONArray("latex", "word", "coverage", "graphviz"),
								"workDoneProgress", false),
						"dapServer", new JSONObject("port", DAPServerSocket.getPort())));
		
		PluginRegistry.getInstance().setPluginCapabilities(cap);

		return cap;
	}
}
