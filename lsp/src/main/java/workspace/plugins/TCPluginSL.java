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

import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import json.JSONArray;
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
					if (STRUCTURED_SYMBOLS)
					{
						 // Add nested structural information, rather than a flat outline.
						results.add(messages.documentSymbols(module, file));
					}
					else
					{
						if (module.name.getLocation().file.equals(file))
						{
							results.add(messages.symbolInformation(module.name.toString(),
								module.name.getLocation(), SymbolKind.Module, null));
						}
	
						for (TCDefinition def: module.defs)
						{
							for (TCDefinition indef: def.getDefinitions())
							{
								if (indef.name != null && indef.location.file.equals(file) && !indef.name.isOld())
								{
									results.add(messages.symbolInformation(indef.name + ":" + indef.getType(),
											indef.location, SymbolKind.kindOf(indef), indef.location.module));
								}
							}
						}
					}
				}
			}
		}
		
		return results;
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
}
