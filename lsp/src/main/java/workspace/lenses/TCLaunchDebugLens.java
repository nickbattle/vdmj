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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePairList;
import com.fujitsu.vdmj.tc.types.TCTypeList;

import json.JSONArray;
import json.JSONObject;

public class TCLaunchDebugLens extends AbstractLaunchDebugLens implements TCCodeLens
{
	@Override
	public JSONArray getDefinitionLenses(TCDefinition def, TCModule module)
	{
		return getDefinitionLenses(def, (TCClassDefinition)null);
	}

	@Override
	public JSONArray getDefinitionLenses(TCDefinition def, TCClassDefinition cls)
	{
		JSONArray results = new JSONArray();
		
		if (isClientType("vscode") && isPublic(def))
		{
			String launchName = null;
			String defaultName = null;
			String applyName = null;
			JSONArray applyArgs = null;
			
			if (def instanceof TCExplicitFunctionDefinition)
			{
				TCExplicitFunctionDefinition exdef = (TCExplicitFunctionDefinition) def;
				applyName = exdef.name.getName();
				launchName = applyName;
				defaultName = exdef.name.getModule();
				TCFunctionType ftype = (TCFunctionType) exdef.type;
				applyArgs = getParams(exdef.paramPatternList.get(0), ftype.parameters);
			}
			else if (def instanceof TCImplicitFunctionDefinition)
			{
				TCImplicitFunctionDefinition imdef = (TCImplicitFunctionDefinition) def;
				
				if (imdef.body != null)
				{
					applyName = imdef.name.getName();
					launchName = applyName;
					defaultName = imdef.name.getModule();
					applyArgs = getParams(imdef.parameterPatterns);
				}
			}
			else if (def instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition) def;
				
				if (!exop.isConstructor)
				{
					applyName = exop.name.getName();
					launchName = applyName;
					defaultName = exop.name.getModule();
					TCOperationType ftype = (TCOperationType) exop.type;
					applyArgs = getParams(exop.parameterPatterns, ftype.parameters);
				}
			}
			else if (def instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition) def;
				
				if (!imop.isConstructor && imop.body != null)
				{
					applyName = imop.name.getName();
					launchName = applyName;
					defaultName = imop.name.getModule();
					applyArgs = getParams(imop.parameterPatterns);
				}
			}
			
			if (launchName != null)
			{
				JSONArray constructors = null;
				
				if (cls != null && !def.isAccess(Token.STATIC))	// Look for class constructors
				{
					constructors = new JSONArray();
					
					for (TCDefinition cdef: cls.definitions)
					{
						if (cdef instanceof TCExplicitOperationDefinition)
						{
							TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition)cdef;
							
							if (exop.isConstructor)
							{
								TCOperationType optype = (TCOperationType) exop.type;
								constructors.add(getParams(exop.parameterPatterns, optype.parameters));
							}
						}
						else if (cdef instanceof TCImplicitOperationDefinition)
						{
							TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition)cdef;
							
							if (imop.isConstructor)
							{
								constructors.add(getParams(imop.parameterPatterns));
							}
						}
					}
					
					if (constructors.isEmpty())
					{
						// create an entry for the default constructor, new C()
						constructors.add(new JSONArray());
					}
				}
			
				results.add(makeLens(def.location, "Launch", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, false, constructors, applyName, applyArgs)));
					
				results.add(makeLens(def.location, "Debug", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, true, constructors, applyName, applyArgs)));
			}
		}

		return results;
	}
	
	private JSONArray getParams(TCPatternList patterns, TCTypeList types)
	{
		JSONArray params = new JSONArray();
		int i = 0;
		
		for (TCPattern p: patterns)
		{
			params.add(new JSONObject("name", p.toString(), "type", types.get(i++).toString()));
		}
		
		return params;
	}
	
	private JSONArray getParams(TCPatternListTypePairList ptList)
	{
		JSONArray params = new JSONArray();

		for (TCPatternListTypePair param: ptList)
		{
			for (TCPattern p: param.patterns)
			{
				params.add(new JSONObject("name", p.toString(), "type", param.type.toString()));
			}
		}
		
		return params;
	}

	private boolean isPublic(TCDefinition def)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			return def.accessSpecifier.access == Token.PUBLIC;	// Not private or protected
		}
		else
		{
			return true;
		}
	}
}
