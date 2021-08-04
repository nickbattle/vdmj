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

public class LSPInitializeResponse extends JSONObject
{
	private static final long serialVersionUID = 1L;
	
	public LSPInitializeResponse()
	{
		put("serverInfo", new JSONObject("name", "VDMJ LSP Server", "version", "0.1"));
		put("capabilities", getServerCapabilities());
	}

	private JSONObject getServerCapabilities()
	{
		JSONObject cap = new JSONObject();
		cap.put("definitionProvider", true);			// Go to definition for F12
		cap.put("documentSymbolProvider", true);		// Symbol information for Outline view

		cap.put("completionProvider",					// Completions
			new JSONObject(
				"triggerCharacters", new JSONArray(".", "`"),
				"resolveProvider", false));
		
		cap.put("textDocumentSync",						// Note: save covered by watched files
			new JSONObject(
				"openClose", true,
				"change", 2		// incremental
			));
		
		cap.put("experimental",
				new JSONObject(
					"proofObligationProvider", true,
					"combinatorialTestProvider", new JSONObject("workDoneProgress", true),
					"translateProvider", new JSONObject(
							"languageId", new JSONArray("latex", "word", "coverage"), "workDoneProgress", false),
					"dapServer", new JSONObject("port", DAPServerSocket.getPort())));

		return cap;
	}
}
