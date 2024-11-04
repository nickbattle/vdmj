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
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.po.types.POPatternListTypePairList;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

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
			String launchName = null;
			String defaultName = null;
			String applyName = null;
			JSONArray applyTypes = null;
			JSONArray applyArgs = null;
			
			if (def instanceof POExplicitFunctionDefinition)
			{
				POExplicitFunctionDefinition exdef = (POExplicitFunctionDefinition) def;
				
				applyName = exdef.name.getName();
				launchName = applyName;
				defaultName = exdef.name.getModule();
				
				if (exdef.typeParams != null)
				{
					applyTypes = new JSONArray();
					
					for (TCType ptype: exdef.typeParams)
					{
						applyTypes.add(ptype.toString());
					}
				}
				
				TCFunctionType ftype = (TCFunctionType) exdef.type;
				applyArgs = getParams(exdef.paramPatternList.get(0), ftype.parameters);
			}
			else if (def instanceof POImplicitFunctionDefinition)
			{
				POImplicitFunctionDefinition imdef = (POImplicitFunctionDefinition) def;
				
				applyName = imdef.name.getName();
				launchName = applyName;
				defaultName = imdef.name.getModule();
				applyArgs = getParams(imdef.parameterPatterns);
			}
			else if (def instanceof POExplicitOperationDefinition)
			{
				POExplicitOperationDefinition exop = (POExplicitOperationDefinition) def;
				
				applyName = exop.name.getName();
				launchName = applyName;
				defaultName = exop.name.getModule();
				TCOperationType ftype = (TCOperationType) exop.type;
				applyArgs = getParams(exop.parameterPatterns, ftype.parameters);
			}
			else if (def instanceof POImplicitOperationDefinition)
			{
				POImplicitOperationDefinition imop = (POImplicitOperationDefinition) def;
				
				applyName = imop.name.getName();
				launchName = applyName;
				defaultName = imop.name.getModule();
				applyArgs = getParams(imop.parameterPatterns);
			}
			
			if (launchName != null)
			{
				JSONArray constructors = null;
				
				results.add(makeLens(po.location, "PO #" + po.number, CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, true, constructors, applyName, applyTypes, applyArgs)));
			}
		}

		return results;
	}
	
	private JSONArray getParams(POPatternList patterns, TCTypeList types)
	{
		JSONArray params = new JSONArray();
		int i = 0;
		
		for (POPattern p: patterns)
		{
			params.add(new JSONObject("name", p.toString(), "type", types.get(i++).toString()));
		}
		
		return params;
	}
	
	private JSONArray getParams(POPatternListTypePairList ptList)
	{
		JSONArray params = new JSONArray();

		for (POPatternListTypePair param: ptList)
		{
			for (POPattern p: param.patterns)
			{
				params.add(new JSONObject("name", p.toString(), "type", param.type.toString()));
			}
		}
		
		return params;
	}
}
