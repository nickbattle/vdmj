/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package workspace.plugins;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCPatternListTypePair;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;

import json.JSONArray;
import json.JSONObject;
import lsp.textdocument.SymbolKind;
import workspace.Log;

abstract public class TCPlugin extends AnalysisPlugin
{
	protected final List<VDMMessage> errs = new Vector<VDMMessage>();
	protected final List<VDMMessage> warns = new Vector<VDMMessage>();
	
	public static TCPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new TCPluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new TCPluginPR();
				
			default:
				Log.error("Unknown dialect " + dialect);
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
	}

	protected TCPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "TC";
	}

	@Override
	public void init()
	{
	}

	public void preCheck()
	{
		errs.clear();
		warns.clear();
	}
	
	public List<VDMMessage> getErrs()
	{
		return errs;
	}
	
	public List<VDMMessage> getWarns()
	{
		return warns;
	}
	
	abstract public <T> T getTC();
	
	abstract public <T> boolean checkLoadedFiles(T ast) throws Exception;

	abstract public JSONArray documentSymbols(File file);

	abstract public TCDefinition findDefinition(File file, int zline, int zcol);

	abstract public TCDefinitionList lookupDefinition(String startsWith);
	
	abstract public void saveDependencies(File saveUri) throws IOException;

	abstract public JSONArray documentLenses(File file);

	/**
	 * Common methods for hierarchical outlines.
	 */
	protected JSONObject documentSymbolsTop(TCDefinition top)
	{
		JSONObject result = null;
		TCDefinitionList alldefs = top.getDefinitions();
		
		if (!alldefs.isEmpty())
		{
			Iterator<TCDefinition> iter = alldefs.iterator();
			JSONArray children = new JSONArray();
			
			if (top instanceof TCStateDefinition)
			{
				result = messages.documentSymbol(
						top.name.getName(),
						"",
						SymbolKind.Struct,
						top.name.getLocation(),
						top.name.getLocation());

				iter.next();	// Ignore state record
			}
			else if (top instanceof TCValueDefinition && alldefs.size() > 1)
			{
				TCValueDefinition vdef = (TCValueDefinition)top;
				
				result = messages.documentSymbol(
						vdef.pattern.toString(),
						"",
						SymbolKind.Struct,
						vdef.location,
						vdef.location);
			}
			else if (top instanceof TCPerSyncDefinition ||
					 top instanceof TCMutexSyncDefinition)
			{
				result = messages.documentSymbol(
						top.toString(),
						"",
						SymbolKind.Enum,
						top.location,
						top.location);
				
				iter.next();	// Ignore def
			}
			else if (top instanceof TCTypeDefinition)
			{
				TCType type = top.getType();
				String detail = null;
				
				if (type instanceof TCNamedType)
				{
					TCNamedType ntype = (TCNamedType)type;
					detail = ntype.type.toString();
				}
				else if (type instanceof TCRecordType)
				{
					TCRecordType rtype = (TCRecordType)type;
					detail = "";
					
					for (TCField field: rtype.fields)
					{
						children.add(messages.documentSymbol(
							field.tag,
							field.type.toString(),
							SymbolKind.Field,
							field.tagname.getLocation(),
							field.tagname.getLocation()));
					}
				}
				
				result = messages.documentSymbol(
					top.name.getName(),
					detail,
					SymbolKind.kindOf(top),
					top.name.getLocation(),
					top.name.getLocation());
				
				iter.next();
			}
			else
			{
				TCDefinition head = iter.next();	// 1st def is usually the root
				result = documentSymbolsDef(head);
			}
			
			if (iter.hasNext())
			{
				while (iter.hasNext())
				{
					TCDefinition def = iter.next();
					
					if (def.name != null && !def.name.isOld())
					{
						children.add(documentSymbolsDef(def));
					}
				}
			}
			
			if (!children.isEmpty())
			{
				result.put("children", children);
			}
		}
		
		return result;
	}

	private JSONObject documentSymbolsDef(TCDefinition def)
	{
		return messages.documentSymbol(
			def.name.getName(),
			def.getType().toString(),
			SymbolKind.kindOf(def),
			def.name.getLocation(),
			def.name.getLocation());
	}
	
	protected final String CODE_LENS_COMMAND = "vdm-vscode.addRunConfiguration";
	
	protected JSONArray launchArgs(TCDefinition def, boolean debug)
	{
		JSONArray result = new JSONArray();
		JSONObject launchArgs = new JSONObject();
		
		launchArgs.put("name", (debug ? "Debug " : "Launch ") + def.name.getName());
		launchArgs.put("defaultName", def.name.getModule());
		launchArgs.put("type", "vdm");
		launchArgs.put("request", "launch");
		launchArgs.put("noDebug", !debug);		// Note: inverted :)
		launchArgs.put("remoteControl", null);
		
		/**
		 * Rather than adding a command, like "print func(nat, nat)", we send back the
		 * name and parameter name/type pairs, to allow the Client to create a GUI.
		 */
		switch (def.kind())
		{
			case "explicit function":
			{
				TCExplicitFunctionDefinition exdef = (TCExplicitFunctionDefinition) def;
				launchArgs.put("applyName", exdef.name.getName());
				JSONArray params = new JSONArray();
				launchArgs.put("applyArgs", params);
				
				TCFunctionType ftype = (TCFunctionType) exdef.getType();
				TCTypeList ptypes = ftype.parameters;
				int i = 0;
				
				for (TCPattern p: exdef.paramPatternList.get(0))	// Curried?
				{
					params.add(new JSONObject("name", p.toString(), "type", ptypes.get(i++).toString()));
				}
				break;
			}
		
			case "implicit function":
			{
				TCImplicitFunctionDefinition imdef = (TCImplicitFunctionDefinition) def;
				launchArgs.put("applyName", imdef.name.getName());
				JSONArray params = new JSONArray();
				launchArgs.put("applyArgs", params);
				
				for (TCPatternListTypePair param: imdef.parameterPatterns)
				{
					for (TCPattern p: param.patterns)
					{
						params.add(new JSONObject("name", p.toString(), "type", param.type.toString()));
					}
				}
				break;
			}
				
			case "explicit operation":
			{
				TCExplicitOperationDefinition exop = (TCExplicitOperationDefinition) def;
				launchArgs.put("applyName", exop.name.getName());
				JSONArray params = new JSONArray();
				launchArgs.put("applyArgs", params);
				
				TCOperationType ftype = (TCOperationType) exop.getType();
				TCTypeList ptypes = ftype.parameters;
				int i = 0;
				
				for (TCPattern p: exop.parameterPatterns)
				{
					params.add(new JSONObject("name", p.toString(), "type", ptypes.get(i++).toString()));
				}
				break;
			}
				
			case "implicit operation":
			{
				TCImplicitOperationDefinition imdef = (TCImplicitOperationDefinition) def;
				launchArgs.put("applyName", imdef.name.getName());
				JSONArray params = new JSONArray();
				launchArgs.put("applyArgs", params);
				
				for (TCPatternListTypePair param: imdef.parameterPatterns)
				{
					for (TCPattern p: param.patterns)
					{
						params.add(new JSONObject("name", p.toString(), "type", param.type.toString()));
					}
				}
				break;
			}
				
			default:
			{
				Log.error("Unknown launch code lens kind: " +  def.kind());
				launchArgs.put("applyName", def.name.getName());
				launchArgs.put("applyArgs", new JSONArray());
				break;
			}
		}

   		result.add(launchArgs);
    	return result;
	}
}
