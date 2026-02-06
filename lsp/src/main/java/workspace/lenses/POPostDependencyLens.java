/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import json.JSONArray;

public class POPostDependencyLens extends CodeLens implements POCodeLens
{
	private final ProofObligationList polist;
	private final PODefinition def;

	public POPostDependencyLens(PODefinition def, ProofObligationList polist)
	{
		this.polist = polist;
		this.def = def;
	}

	@Override
	public JSONArray getLaunchLens()
	{
		JSONArray results = new JSONArray();
		
		if (isClientType("vscode"))		// Lens is VSCode-specific, because of the command used
		{
			JSONArray args = new JSONArray();

			args.add(def.name.getName());

			for (ProofObligation po: polist)
			{
				args.add(po.number);
			}

			results.add(makeLens(def.location, "Dep POs", "vdm-vscode.showPODependencies", args));
		}
		
		return results;
	}
}
