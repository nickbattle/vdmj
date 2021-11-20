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
import java.util.List;
import java.util.Vector;

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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCTypeList;

import json.JSONArray;
import json.JSONObject;

public class LaunchDebugLens extends CodeLens
{
	private final String CODE_LENS_COMMAND = "vdm-vscode.addRunConfiguration";

	@Override
	public JSONArray codeLenses(ASTDefinition def, File file)
	{
		JSONArray results = new JSONArray();
		
		if ("vscode".equals(getClientName()) && isPublic(def))
		{
			String launchName = null;
			String defaultName = null;
			String applyName = null;
			List<Param> applyArgs = new Vector<Param>();
			
			if (def instanceof ASTExplicitFunctionDefinition)
			{
				ASTExplicitFunctionDefinition exdef = (ASTExplicitFunctionDefinition) def;
				applyName = exdef.name.getName();
				launchName = applyName;
				defaultName = exdef.name.module;
				applyArgs = new Vector<Param>();
				
				ASTFunctionType ftype = (ASTFunctionType) exdef.type;
				ASTTypeList ptypes = ftype.parameters;
				int i = 0;
				
				for (ASTPattern p: exdef.paramPatternList.get(0))	// Curried?
				{
					applyArgs.add(new Param(p.toString(), ptypes.get(i++).toString()));
				}
				
			}
			else if (def instanceof ASTImplicitFunctionDefinition)
			{
				ASTImplicitFunctionDefinition imdef = (ASTImplicitFunctionDefinition) def;
				applyName = imdef.name.getName();
				launchName = applyName;
				defaultName = imdef.name.module;
				applyArgs = new Vector<Param>();
				
				for (ASTPatternListTypePair param: imdef.parameterPatterns)
				{
					for (ASTPattern p: param.patterns)
					{
						applyArgs.add(new Param(p.toString(), param.type.toString()));
					}
				}
			}
			else if (def instanceof ASTExplicitOperationDefinition)
			{
				ASTExplicitOperationDefinition exop = (ASTExplicitOperationDefinition) def;
				applyName = exop.name.getName();
				launchName = applyName;
				defaultName = exop.name.module;
				applyArgs = new Vector<Param>();
				
				ASTOperationType ftype = (ASTOperationType) exop.type;
				ASTTypeList ptypes = ftype.parameters;
				int i = 0;
				
				for (ASTPattern p: exop.parameterPatterns)
				{
					applyArgs.add(new Param(p.toString(), ptypes.get(i++).toString()));
				}
			}
			else if (def instanceof ASTImplicitOperationDefinition)
			{
				ASTImplicitOperationDefinition imop = (ASTImplicitOperationDefinition) def;
				applyName = imop.name.getName();
				launchName = applyName;
				defaultName = imop.name.module;
				applyArgs = new Vector<Param>();
				
				for (ASTPatternListTypePair param: imop.parameterPatterns)
				{
					for (ASTPattern p: param.patterns)
					{
						applyArgs.add(new Param(p.toString(), param.type.toString()));
					}
				}
			}
			
			if (launchName != null)
			{
				results.add(makeLens(def.location, "Launch", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, false, applyName, applyArgs)));
					
				results.add(makeLens(def.location, "Debug", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, true, applyName, applyArgs)));
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
			List<Param> applyArgs = new Vector<Param>();
			
			if (def instanceof TCExplicitFunctionDefinition)
			{
				TCExplicitFunctionDefinition exdef = (TCExplicitFunctionDefinition) def;
				applyName = exdef.name.getName();
				launchName = applyName;
				defaultName = exdef.name.getModule();
				applyArgs = new Vector<Param>();
				
				TCFunctionType ftype = (TCFunctionType) exdef.type;
				TCTypeList ptypes = ftype.parameters;
				int i = 0;
				
				for (TCPattern p: exdef.paramPatternList.get(0))	// Curried?
				{
					applyArgs.add(new Param(p.toString(), ptypes.get(i++).toString()));
				}
				
			}
			else if (def instanceof TCImplicitFunctionDefinition)
			{
				TCImplicitFunctionDefinition imdef = (TCImplicitFunctionDefinition) def;
				applyName = imdef.name.getName();
				launchName = applyName;
				defaultName = imdef.name.getModule();
				applyArgs = new Vector<Param>();
				
				for (TCPatternListTypePair param: imdef.parameterPatterns)
				{
					for (TCPattern p: param.patterns)
					{
						applyArgs.add(new Param(p.toString(), param.type.toString()));
					}
				}
			}
			else if (def instanceof TCExplicitOperationDefinition)
			{
				TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition) def;
				applyName = exop.name.getName();
				launchName = applyName;
				defaultName = exop.name.getModule();
				applyArgs = new Vector<Param>();
				
				TCOperationType ftype = (TCOperationType) exop.type;
				TCTypeList ptypes = ftype.parameters;
				int i = 0;
				
				for (TCPattern p: exop.parameterPatterns)
				{
					applyArgs.add(new Param(p.toString(), ptypes.get(i++).toString()));
				}
			}
			else if (def instanceof TCImplicitOperationDefinition)
			{
				TCImplicitOperationDefinition imop = (TCImplicitOperationDefinition) def;
				applyName = imop.name.getName();
				launchName = applyName;
				defaultName = imop.name.getModule();
				applyArgs = new Vector<Param>();
				
				for (TCPatternListTypePair param: imop.parameterPatterns)
				{
					for (TCPattern p: param.patterns)
					{
						applyArgs.add(new Param(p.toString(), param.type.toString()));
					}
				}
			}
			
			if (launchName != null)
			{
				results.add(makeLens(def.location, "Launch", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, false, applyName, applyArgs)));
					
				results.add(makeLens(def.location, "Debug", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, true, applyName, applyArgs)));
			}
		}

		return results;
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

	private static class Param
	{
		public final String name;
		public final String type;
		
		public Param(String name, String type)
		{
			this.name = name;
			this.type = type;
		}
		
		public JSONObject toJSON()
		{
			return new JSONObject("name", name, "type", type);
		}
	}

	private JSONArray launchArgs(String launchName, String defaultName,
			boolean debug, String applyName, List<Param> applyArgs)
	{
		JSONObject launchArgs = new JSONObject();
		
		launchArgs.put("name", (debug ? "Debug " : "Launch ") + launchName);
		launchArgs.put("defaultName", defaultName);
		launchArgs.put("type", "vdm");
		launchArgs.put("request", "launch");
		launchArgs.put("noDebug", !debug);		// Note: inverted :)
		launchArgs.put("remoteControl", null);
		
		launchArgs.put("applyName", applyName);
		JSONArray args = new JSONArray();
		launchArgs.put("applyArgs", args);
		
		for (Param p: applyArgs)
		{
			args.add(p.toJSON());
		}

    	return new JSONArray(launchArgs);
	}
}
