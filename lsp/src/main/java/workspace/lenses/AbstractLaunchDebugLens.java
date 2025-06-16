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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace.lenses;

import json.JSONArray;
import json.JSONObject;

public abstract class AbstractLaunchDebugLens extends CodeLens
{
	protected final String CODE_LENS_COMMAND = "vdm-vscode.addLensRunConfiguration";

	/**
	 * Generate the command arguments for a code lens. This is used as the "arguments" passed to
	 * the makeLens methods to create the response to the Client.
	 */
	protected JSONArray launchArgs(String launchName, String defaultName,
			boolean debug, JSONArray constructors, String applyName, JSONArray applyTypes, JSONArray applyArgs)
	{
		JSONObject launchArgs = new JSONObject();
		
		launchArgs.put("name", (debug ? "Debug " : "Launch ") + launchName);
		launchArgs.put("defaultName", defaultName);
		launchArgs.put("type", "vdm");
		launchArgs.put("request", "launch");
		launchArgs.put("noDebug", !debug);		// Note: inverted :)
		launchArgs.put("remoteControl", null);
		
		if (constructors != null && !constructors.isEmpty())
		{
			launchArgs.put("constructors", constructors);
		}
		
		launchArgs.put("applyName", applyName);
		
		if (applyTypes != null)
		{
			launchArgs.put("applyTypes", applyTypes);
		}
		
		launchArgs.put("applyArgs", applyArgs);

    	return new JSONArray(launchArgs);	// Array with one object
	}
}
