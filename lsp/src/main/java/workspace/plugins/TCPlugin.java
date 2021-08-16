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
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCType;

import json.JSONArray;
import json.JSONObject;
import lsp.textdocument.SymbolKind;

abstract public class TCPlugin extends AnalysisPlugin
{
	protected final List<VDMMessage> errs = new Vector<VDMMessage>();
	protected final List<VDMMessage> warns = new Vector<VDMMessage>();
	
	public TCPlugin()
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
			
			if (top instanceof TCStateDefinition)
			{
				result = documentSymbolsDef(top);
				iter.next();	// Ignore state record
			}
			else if (top instanceof TCValueDefinition && alldefs.size() > 1)
			{
				TCValueDefinition vdef = (TCValueDefinition)top;
				
				result = messages.documentSymbol(
						vdef.pattern.toString(),
						"",
						SymbolKind.Array,
						vdef.location,
						vdef.location);
			}
			else if (top instanceof TCPerSyncDefinition || top instanceof TCMutexSyncDefinition)
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
				else
				{
					detail = type.toString();
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
		return messages.documentSymbol(
			def.name.getName(),
			def.getType().toString(),
			SymbolKind.kindOf(def),
			def.name.getLocation(),
			def.name.getLocation());
	}
}
