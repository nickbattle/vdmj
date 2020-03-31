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

package lsp;

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
		cap.put("definitionProvider", true);			// Go to definition
		// cap.put("typeDefinitionProvider", true);		// Go to type?
		cap.put("documentSymbolProvider", true);		// Symbol information
		
		cap.put("textDocumentSync",
			new JSONObject(
				"openClose", true,
				"change", 2,
				"save", new JSONObject("includeText", false)));
		
		return cap;
	}
}
