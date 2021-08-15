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

package lsp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinitionList;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTPerSyncDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTStateDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTValueDefinition;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.types.ASTField;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.types.TCType;

import json.JSONArray;
import json.JSONObject;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class LSPMessageUtils
{
	public RPCMessageList diagnosticResponses(List<? extends VDMMessage> list) throws IOException
	{
		return diagnosticResponses(list, (Set<File>)null);	// All files mention in list
	}
	
	public RPCMessageList diagnosticResponses(List<? extends VDMMessage> list, File file) throws IOException
	{
		Set<File> filesToReport = new HashSet<File>();
		filesToReport.add(file);
		return diagnosticResponses(list, filesToReport);
	}
	
	public RPCMessageList diagnosticResponses(List<? extends VDMMessage> list, Set<File> filesToReport) throws IOException
	{
		Map<File, List<VDMMessage>> map = new HashMap<File, List<VDMMessage>>();
		
		for (VDMMessage message: list)
		{
			File file = message.location.file.getAbsoluteFile();
			List<VDMMessage> set = map.get(file);
			
			if (set == null)
			{
				set = new Vector<VDMMessage>();
				set.add(message);
				map.put(file, set);
			}
			else
			{
				set.add(message);
			}
		}
		
		RPCMessageList responses = new RPCMessageList();
		
		if (filesToReport == null)	// All of the file mentioned
		{
			filesToReport = map.keySet();
		}
		
		for (File file: filesToReport)
		{
			JSONArray messages = new JSONArray();
			
			if (map.containsKey(file))
			{
				for (VDMMessage message: map.get(file))
				{
					messages.add(
						new JSONObject(
							"range",	Utils.lexLocationToRange(message.location),
							"severity", (message instanceof VDMError ? 1 : 2),
							"code", 	message.number,
							"message",	message.toProblemString().replaceAll("\n", ", ")));
					
				}
			}
			
			JSONObject params = new JSONObject("uri", file.toURI().toString(), "diagnostics", messages);
			responses.add(RPCRequest.notification("textDocument/publishDiagnostics", params));
		}
		
		return responses;
	}
	
	public Set<File> filesOfMessages(List<VDMMessage> list)
	{
		Set<File> set = new HashSet<File>();
		
		for (VDMMessage message: list)
		{
			set.add(message.location.file.getAbsoluteFile());
		}

		return set;
	}
	
	public JSONObject symbolInformation(String name, LexLocation location, SymbolKind kind, String container)
	{
		JSONObject sym = new JSONObject(
			"name", name,
			"kind", kind.getValue(),
			"location", Utils.lexLocationToLocation(location));
		
		if (container != null)
		{
			sym.put("container", container);
		}
		
		return sym;
	}

	public JSONObject symbolInformation(LexIdentifierToken name, SymbolKind kind, String container)
	{
		return symbolInformation(name.name, name.location, kind, container);
	}
	
	public JSONObject symbolInformation(LexIdentifierToken name, TCType type, SymbolKind kind, String container)
	{
		return symbolInformation(name.name + ":" + type, name.location, kind, container);
	}
	
	/**
	 * These methods produce a hierarchical outline
	 */
	public JSONObject documentSymbols(TCModule module, File file)
	{
		LexLocation from = null;
		LexLocation to = null;
		TCDefinitionList list = new TCDefinitionList();

		for (TCDefinition def: module.defs)
		{
			if (def.location.file.equals(file))	// DEFAULT spans files
			{
				if (from == null)
				{
					from = def.location;
					to = def.location;
				}
				else
				{
					to = def.location;
				}
				
				list.add(def);
			}
		}
		
		return new JSONObject(
			"name",				module.name.getName(),
			"kind",				SymbolKind.Module.getValue(),
			"range",			Utils.lexLocationsToRange(from, to),
			"selectionRange",	Utils.lexLocationsToRange(from, to),
			"children",			documentSymbols(list));
	}

	public JSONObject documentSymbols(TCClassDefinition clazz)
	{
		return new JSONObject(
			"name",				clazz.name.getName(),
			"kind",				SymbolKind.Class.getValue(),
			"range",			Utils.lexLocationToRange(LexLocation.getSpan(clazz.name.getLex())),
			"selectionRange",	Utils.lexLocationToRange(clazz.name.getLocation()),
			"children",			documentSymbols(clazz.definitions));
	}

	private JSONArray documentSymbols(TCDefinitionList defs)
	{
		JSONArray symbols = new JSONArray();

		for (TCDefinition def: defs)
		{
			JSONObject symbol = documentSymbolsTop(def);
			if (symbol != null) symbols.add(symbol);
		}
		
		return symbols;
	}

	private JSONObject documentSymbolsTop(TCDefinition top)
	{
		JSONObject result = null;
		TCDefinitionList alldefs = top.getDefinitions();
		
		if (!alldefs.isEmpty())
		{
			Iterator<TCDefinition> iter = alldefs.iterator();
			
			if (top instanceof TCStateDefinition)
			{
				result = documentSymbolsDef(top);
				iter.next();	// Ignore state record
			}
			else if (top instanceof TCValueDefinition && alldefs.size() > 1)
			{
				TCValueDefinition vdef = (TCValueDefinition)top;
				
				result = new JSONObject(
						"name",				vdef.pattern.toString(),
						"kind",				SymbolKind.Array.getValue(),
						"range",			Utils.lexLocationToRange(vdef.location),
						"selectionRange",	Utils.lexLocationToRange(vdef.location));
			}
			else if (top instanceof TCPerSyncDefinition || top instanceof TCMutexSyncDefinition)
			{
				result = new JSONObject(
						"name",				top.toString(),
						"kind",				SymbolKind.Enum.getValue(),
						"range",			Utils.lexLocationToRange(top.location),
						"selectionRange",	Utils.lexLocationToRange(top.location));
				
				iter.next();	// Ignore def
			}
			else
			{
				TCDefinition head = iter.next();	// 1st def is usually the root
				result = documentSymbolsDef(head);
			}
			
			if (iter.hasNext())
			{
				JSONArray children = new JSONArray();
				
				while (iter.hasNext())
				{
					TCDefinition def = iter.next();
					
					if (def.name != null && !def.name.isOld())
					{
						children.add(documentSymbolsDef(def));
					}
				}
				
				result.put("children", children);
			}
		}
		
		return result;
	}

	private JSONObject documentSymbolsDef(TCDefinition def)
	{
		return new JSONObject(
			"name",				def.name.getName(),
			"detail",			def.getType().toString(),
			"kind",				SymbolKind.kindOf(def).getValue(),
			"range",			Utils.lexLocationToRange(def.name.getLocation()),
			"selectionRange",	Utils.lexLocationToRange(def.name.getLocation()));
	}

	/**
	 * Structured outlines for an AST tree, as far as we can. 
	 */
	public JSONObject documentSymbols(ASTModule module, File file)
	{
		LexLocation from = null;
		LexLocation to = null;
		ASTDefinitionList list = new ASTDefinitionList();

		for (ASTDefinition def: module.defs)
		{
			if (def.location.file.equals(file))	// DEFAULT spans files
			{
				if (from == null)
				{
					from = def.location;
					to = def.location;
				}
				else
				{
					to = def.location;
				}
				
				list.add(def);
			}
		}
		
		return new JSONObject(
			"name",				module.name.name,
			"kind",				SymbolKind.Module.getValue(),
			"range",			Utils.lexLocationsToRange(from, to),
			"selectionRange",	Utils.lexLocationsToRange(from, to),
			"children",			documentSymbols(list));
	}

	public JSONObject documentSymbols(ASTClassDefinition clazz)
	{
		return new JSONObject(
			"name",				clazz.name.getName(),
			"kind",				SymbolKind.Class.getValue(),
			"range",			Utils.lexLocationToRange(clazz.name.location),
			"selectionRange",	Utils.lexLocationToRange(clazz.name.location),
			"children",			documentSymbols(clazz.definitions));
	}

	private JSONArray documentSymbols(ASTDefinitionList defs)
	{
		JSONArray symbols = new JSONArray();

		for (ASTDefinition def: defs)
		{
			symbols.add(documentSymbolsDef(def));
		}
		
		return symbols;
	}

	private JSONObject documentSymbolsDef(ASTDefinition def)
	{
		String name = def.name == null ? null : def.name.toString();
		
		if (name == null)
		{
			if (def instanceof ASTValueDefinition)
			{
				ASTValueDefinition vdef = (ASTValueDefinition)def;
				name = vdef.pattern.toString();
			}
			else
			{
				name = def.toString();
			}
		}
		
		String detail = "";
		JSONArray children = null;
		
		if (def instanceof ASTExplicitFunctionDefinition)
		{
			ASTExplicitFunctionDefinition fdef = (ASTExplicitFunctionDefinition)def;
			detail = fdef.type.toString();
		}
		else if (def instanceof ASTImplicitFunctionDefinition)
		{
			ASTImplicitFunctionDefinition fdef = (ASTImplicitFunctionDefinition)def;
			detail = fdef.parameterPatterns + " ==> " + fdef.result;
		}
		else if (def instanceof ASTExplicitOperationDefinition)
		{
			ASTExplicitOperationDefinition opdef = (ASTExplicitOperationDefinition)def;
			detail = opdef.type.toString();
		}
		else if (def instanceof ASTImplicitOperationDefinition)
		{
			ASTImplicitOperationDefinition opdef = (ASTImplicitOperationDefinition)def;
			detail = opdef.parameterPatterns + " ==> " + opdef.result;
		}
		else if (def instanceof ASTStateDefinition)
		{
			ASTStateDefinition sdef = (ASTStateDefinition)def;
			children = new JSONArray();
			
			for (ASTField field: sdef.fields)
			{
				children.add(new JSONObject(
						"name",				field.tag,
						"detail",			field.type.toString(),
						"kind",				SymbolKind.Field.getValue(),
						"range",			Utils.lexLocationToRange(field.tagname.location),
						"selectionRange",	Utils.lexLocationToRange(field.tagname.location)));
			}
		}
		else if (def instanceof ASTPerSyncDefinition)
		{
			name = def.toString();
		}
		
		JSONObject result = new JSONObject(
			"name",				name,
			"detail",			detail,
			"kind",				SymbolKind.kindOf(def).getValue(),
			"range",			Utils.lexLocationToRange(def.location),
			"selectionRange",	Utils.lexLocationToRange(def.location));
		
		if (children != null)
		{
			result.put("children", children);
		}
		
		return result;
	}
}
