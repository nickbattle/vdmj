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

import json.JSONArray;
import json.JSONObject;

public abstract class AbstractLaunchDebugLens extends CodeLens
{
	@Override
	abstract public <DEF> JSONArray getDefinitionLenses(DEF definition);

	protected JSONArray launchArgs(String launchName, String defaultName,
			boolean debug, JSONArray constructors, String applyName, JSONArray applyArgs)
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
		launchArgs.put("applyArgs", applyArgs);

    	return new JSONArray(launchArgs);	// Array with one object
	}
}
