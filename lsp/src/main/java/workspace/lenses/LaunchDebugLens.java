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

import java.io.File;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTOperationType;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePair;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePairList;
import com.fujitsu.vdmj.tc.types.TCTypeList;

import json.JSONArray;
import json.JSONObject;

public class LaunchDebugLens extends CodeLens
{
	private final String CODE_LENS_COMMAND = "vdm-vscode.addLensRunConfiguration";
	// private final String CODE_LENS_COMMAND = "workbench.action.debug.configure";

	@Override
	public JSONArray codeLenses(ASTDefinition def, File file)
	{
		JSONArray results = new JSONArray();
		
		if ("vscode".equals(getClientName()) && isPublic(def))
		{
			String launchName = null;
			String defaultName = null;
			String applyName = null;
			JSONArray applyArgs = new JSONArray();
			
			if (def instanceof ASTExplicitFunctionDefinition)
			{
				ASTExplicitFunctionDefinition exdef = (ASTExplicitFunctionDefinition) def;
				applyName = exdef.name.getName();
				launchName = applyName;
				defaultName = exdef.name.module;
				
				ASTFunctionType ftype = (ASTFunctionType) exdef.type;
				ASTTypeList ptypes = ftype.parameters;
				int i = 0;
				
				for (ASTPattern p: exdef.paramPatternList.get(0))	// Curried?
				{
					applyArgs.add(new JSONObject("name", p.toString(), "type", ptypes.get(i++).toString()));
				}
			}
			else if (def instanceof ASTImplicitFunctionDefinition)
			{
				ASTImplicitFunctionDefinition imdef = (ASTImplicitFunctionDefinition) def;
				
				if (imdef.body != null)
				{
					applyName = imdef.name.getName();
					launchName = applyName;
					defaultName = imdef.name.module;
					
					for (ASTPatternListTypePair param: imdef.parameterPatterns)
					{
						for (ASTPattern p: param.patterns)
						{
							applyArgs.add(new JSONObject("name", p.toString(), "type", param.type.toString()));
						}
					}
				}
			}
			else if (def instanceof ASTExplicitOperationDefinition)
			{
				ASTExplicitOperationDefinition exop = (ASTExplicitOperationDefinition) def;
				applyName = exop.name.getName();
				defaultName = exop.name.module;
				
				if (!applyName.equals(defaultName))		// Not a constructor
				{
					launchName = applyName;
					
					ASTOperationType ftype = (ASTOperationType) exop.type;
					ASTTypeList ptypes = ftype.parameters;
					int i = 0;
					
					for (ASTPattern p: exop.parameterPatterns)
					{
						applyArgs.add(new JSONObject("name", p.toString(), "type", ptypes.get(i++).toString()));
					}
				}
			}
			else if (def instanceof ASTImplicitOperationDefinition)
			{
				ASTImplicitOperationDefinition imop = (ASTImplicitOperationDefinition) def;
				applyName = imop.name.getName();
				defaultName = imop.name.module;

				if (!applyName.equals(defaultName) && imop.body != null)	// Not a constructor
				{
					launchName = applyName;
					
					for (ASTPatternListTypePair param: imop.parameterPatterns)
					{
						for (ASTPattern p: param.patterns)
						{
							applyArgs.add(new JSONObject("name", p.toString(), "type", param.type.toString()));
						}
					}
				}
			}
			
			if (launchName != null)
			{
				results.add(makeLens(def.location, "Launch", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, false, null, applyName, applyArgs)));
					
				results.add(makeLens(def.location, "Debug", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, true, null, applyName, applyArgs)));
			}
		}
		
		return results;
	}
	
	@Override
	public JSONArray codeLenses(TCDefinition def, File file)
	{
		JSONArray results = new JSONArray();
		
		if ("vscode".equals(getClientName()) && isPublic(def))
		{
			String launchName = null;
			String defaultName = null;
			String applyName = null;
			JSONArray applyArgs = null;
			TCClassDefinition classdef = null;
			
			if (def instanceof TCExplicitFunctionDefinition)
			{
				TCExplicitFunctionDefinition exdef = (TCExplicitFunctionDefinition) def;
				applyName = exdef.name.getName();
				launchName = applyName;
				defaultName = exdef.name.getModule();
				TCFunctionType ftype = (TCFunctionType) exdef.type;
				applyArgs = getParams(exdef.paramPatternList.get(0), ftype.parameters);
				classdef = exdef.classDefinition;
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
					classdef = imdef.classDefinition;
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
					classdef = exop.classDefinition;
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
					classdef = imop.classDefinition;
				}
			}
			
			if (launchName != null)
			{
				JSONArray constructors = null;
				
				if (classdef != null && !def.isAccess(Token.STATIC))	// Look for class constructors
				{
					constructors = new JSONArray();
					
					for (TCDefinition cdef: classdef.definitions)
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

		
	private boolean isPublic(ASTDefinition def)
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

	private JSONArray launchArgs(String launchName, String defaultName,
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
