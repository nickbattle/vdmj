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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;

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
	
	public JSONObject documentSymbol(String name, String detail,
			SymbolKind kind, LexLocation range, LexLocation selection)
	{
		return new JSONObject(
			"name",				name,
			"detail",			detail,
			"kind",				kind.getValue(),
			"range",			Utils.lexLocationToRange(range),
			"selectionRange",	Utils.lexLocationToRange(selection));
	}
	
	public JSONObject documentSymbol(String name, String detail,
			SymbolKind kind, LexLocation range, LexLocation selection, JSONArray children)
	{
		if (children == null)
		{
			return documentSymbol(name, detail, kind, range, selection);
		}
		else
		{
			return new JSONObject(
				"name",				name,
				"detail",			detail,
				"kind",				kind.getValue(),
				"range",			Utils.lexLocationToRange(range),
				"selectionRange",	Utils.lexLocationToRange(selection),
				"children",			children);
		}
	}

	public JSONObject typeHierarchyItem(TCClassDefinition cdef)
	{
		return new JSONObject(
				"name",				cdef.name.getName(),
				"kind",				SymbolKind.Class,
				"uri",				cdef.location.file.toURI().toString(),
				"range",			Utils.lexLocationToRange(cdef.location),
				"selectionRange",	Utils.lexLocationToRange(cdef.location));
	}
	
	public JSONArray typeHierarchyItems(TCClassList cdefs)
	{
		JSONArray results = new JSONArray();
		
		for (TCClassDefinition cdef: cdefs)
		{
			results.add(typeHierarchyItem(cdef));
		}
		
		return results;
	}
}
