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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.util.DependencyOrder;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import lsp.textdocument.SymbolKind;
import vdmj.LSPDefinitionFinder;

public class TCPluginSL extends TCPlugin
{
	private TCModuleList tcModuleList = null;
	
	public TCPluginSL()
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

	@Override
	public void preCheck()
	{
		super.preCheck();
		tcModuleList = new TCModuleList();
	}
	
	@Override
	public <T> boolean checkLoadedFiles(T astModuleList) throws Exception
	{
		try
		{
			tcModuleList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(astModuleList);
			tcModuleList.combineDefaults();
			TypeChecker tc = new ModuleTypeChecker(tcModuleList);
			tc.typeCheck();
		}
		catch (InternalException e)
		{
			if (e.number != 10)		// Too many errors
			{
				throw e;
			}
		}
		
		if (TypeChecker.getErrorCount() > 0)
		{
			errs.addAll(TypeChecker.getErrors());
		}
		
		if (TypeChecker.getWarningCount() > 0)
		{
			warns.addAll(TypeChecker.getWarnings());
		}
		
		return errs.isEmpty();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getTC()
	{
		return (T)tcModuleList;
	}
	
	@Override
	public JSONArray documentSymbols(File file)
	{
		JSONArray results = new JSONArray();
		
		if (!tcModuleList.isEmpty())	// May be syntax errors
		{
			for (TCModule module: tcModuleList)
			{
				if (module.files.contains(file))
				{
					results.add(documentSymbols(module, file));
				}
			}
		}
		
		return results;
	}
	
	private JSONObject documentSymbols(TCModule module, File file)
	{
		JSONArray symbols = new JSONArray();

		for (TCDefinition def: module.defs)
		{
			if (def.location.file.equals(file))		// DEFAULT module spans files
			{
				JSONObject symbol = documentSymbolsTop(def);
				if (symbol != null) symbols.add(symbol);
			}
		}
		
		LexLocation location = module.name.getLocation();
		
		if (location.file.getName().equals("?"))	// A combined default module
		{
			location = new LexLocation(file, "DEFAULT", 1, 1, 1, 1);
		}

		return messages.documentSymbol(
			module.name.getName(),
			"",
			SymbolKind.Module,
			location,
			location,
			symbols);
	}

	@Override
	public TCDefinition findDefinition(File file, int zline, int zcol)
	{
		if (tcModuleList != null && !tcModuleList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcModuleList, file, zline + 1, zcol + 1);
		}
		else
		{
			return null;
		}
	}

	@Override
	public TCDefinitionList lookupDefinition(String startsWith)
	{
		TCDefinitionList results = new TCDefinitionList();
		
		for (TCModule module: tcModuleList)
		{
			if (module.name.getName().startsWith(startsWith))
			{
				for (TCDefinition def: module.defs.singleDefinitions())
				{
					if (def.name != null)
					{
						results.add(def);
					}
				}
			}
			else
			{
				for (TCDefinition def: module.defs.singleDefinitions())
				{
					if (def.name != null && def.name.getName().startsWith(startsWith))
					{
						results.add(def);
					}
				}
			}
		}
		
		return results;
	}

	@Override
	public void saveDependencies(File saveUri) throws IOException
	{
		if (tcModuleList != null)
		{
			DependencyOrder order = new DependencyOrder();
			order.moduleOrder(tcModuleList);
			order.graphOf(saveUri);
		}
	}

	@Override
	public JSONArray documentLenses(File file)
	{
		JSONArray results = new JSONArray();
		
		if (!tcModuleList.isEmpty())
		{
			for (TCModule module: tcModuleList)
			{
				if (module.files.contains(file))
				{
					for (TCDefinition def: module.defs)
					{
						if (def.isCallableFunction() || def.isCallableOperation())
						{
							results.add(
								new JSONObject(
									"range", Utils.lexLocationToRange(def.location),
									"command", new JSONObject("title", "Launch", "command", "???")));
							
							results.add(
								new JSONObject(
									"range", Utils.lexLocationToRange(def.location),
									"command", new JSONObject("title", "Debug", "command", "???")));
						}
					}
				}
			}
		}
		
		return results;
	}
}
