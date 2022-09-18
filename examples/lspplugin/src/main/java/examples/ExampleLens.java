/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package examples;

import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.modules.TCModule;

import json.JSONArray;
import workspace.lenses.CodeLens;
import workspace.lenses.TCCodeLens;

/**
 * Most code lenses extend CodeLens, which provides makeLens methods to create the
 * JSON response(s). But this is only a convenience. The way a plugin implements
 * its code lenses is just a matter for the applyCodeLenses method in the plugin.
 */
public class ExampleLens extends CodeLens implements TCCodeLens
{
	@Override
	public JSONArray getDefinitionLenses(TCDefinition def, TCModule module)
	{
		return getDefinitionLenses(def, (TCClassDefinition)null);
	}

	@Override
	public JSONArray getDefinitionLenses(TCDefinition definition, TCClassDefinition classdef)
	{
		JSONArray results = new JSONArray();
		
		if (definition instanceof TCExplicitFunctionDefinition)	// Only explicit functions (say)
		{
			TCExplicitFunctionDefinition def = (TCExplicitFunctionDefinition)definition;
			// This displays the launch.json file via a "Config" lens
			results.add(makeLens(def.location, "Config", "workbench.action.debug.configure"));
		}
		
		return results;
	}
}
