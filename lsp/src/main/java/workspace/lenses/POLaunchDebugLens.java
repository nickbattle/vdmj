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

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.pog.ProofObligation;

import json.JSONArray;

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
}
