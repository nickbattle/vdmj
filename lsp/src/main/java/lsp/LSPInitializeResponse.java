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

package lsp;

import json.JSONObject;
import workspace.PluginRegistry;

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
		JSONObject cap = new JSONObject("experimental", new JSONObject());
		PluginRegistry.getInstance().setLSPCapabilities(cap);
		return cap;
	}
}
