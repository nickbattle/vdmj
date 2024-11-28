/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

import java.util.List;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.pog.POLaunchFactory;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.POLaunchFactory.ApplyArg;
import com.fujitsu.vdmj.pog.POLaunchFactory.ApplyCall;

import json.JSONArray;
import json.JSONObject;

/**
 * A class to generate launch lenses for PO counterexamples.
 */
public class POLaunchDebugLens extends AbstractLaunchDebugLens
{
	private final ProofObligation po;
	
	public POLaunchDebugLens(ProofObligation po)
	{
		this.po = po;
	}

	public JSONArray getLaunchLens()
	{
		JSONArray results = new JSONArray();
		PODefinition def = po.definition;
		
		if (isClientType("vscode") && def != null)
		{
			results.add(
				makeLens(po.location, "PO #" + po.number, CODE_LENS_COMMAND,
					launchArgs(po, def.location.module, true)));
		}

		return results;
	}
	
	private JSONArray launchArgs(ProofObligation po, String defaultName, boolean debug)
	{
		JSONObject launchArgs = new JSONObject();
		POLaunchFactory factory = new POLaunchFactory(po);
		
		ApplyCall apply = factory.getCexApply();
		
		launchArgs.put("name", "PO #" + po.number);
		launchArgs.put("defaultName", defaultName);
		launchArgs.put("type", "vdm");
		launchArgs.put("request", "launch");
		launchArgs.put("noDebug", !debug);		// Note: inverted :)
		launchArgs.put("remoteControl", null);
		launchArgs.put("params", new JSONObject("type", "PO_LENS"));
		
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
		
		for (List<ApplyArg> arglist: apply.applyArgs)
		{
			JSONArray sublist = new JSONArray();
			
			for (ApplyArg arg: arglist)
			{
				sublist.add(new JSONObject("name", arg.name, "type", arg.type, "value", arg.value));
			}
			
			applyArgs.add(sublist);
		}
		
		launchArgs.put("applyArgs", applyArgs);

    	return new JSONArray(launchArgs);
	}
}
