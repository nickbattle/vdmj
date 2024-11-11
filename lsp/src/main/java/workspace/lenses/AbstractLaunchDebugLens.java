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

import com.fujitsu.vdmj.pog.POLaunchFactory;
import com.fujitsu.vdmj.pog.POLaunchFactory.ApplyArg;
import com.fujitsu.vdmj.pog.POLaunchFactory.ApplyCall;
import com.fujitsu.vdmj.pog.ProofObligation;

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
	
	protected JSONArray launchArgs(ProofObligation po, String defaultName, boolean debug)
	{
		JSONObject launchArgs = new JSONObject();
		POLaunchFactory factory = new POLaunchFactory(po);
		
		ApplyCall apply = factory.getCexApply();
		
		launchArgs.put("name", (debug ? "Debug " : "Launch ") + apply.applyName);
		launchArgs.put("defaultName", defaultName);
		launchArgs.put("type", "vdm");
		launchArgs.put("request", "launch");
		launchArgs.put("noDebug", !debug);		// Note: inverted :)
		launchArgs.put("remoteControl", null);
		
		launchArgs.put("applyName", apply.applyName);
		
		if (!apply.applyTypes.isEmpty())
		{
			JSONArray applyTypes = new JSONArray();
			
			for (String atype: apply.applyTypes)
			{
				applyTypes.add(atype);
			}
			
			launchArgs.put("applyTypes", applyTypes);
		}
		
		JSONArray applyArgs = new JSONArray();
		
		for (ApplyArg arg: apply.applyArgs)
		{
			applyArgs.add(new JSONObject("name", arg.name, "type", arg.type, "value", arg.value));
		}
		
		launchArgs.put("applyArgs", applyArgs);

    	return new JSONArray(launchArgs);
	}
	
}
