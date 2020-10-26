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
 *
 ******************************************************************************/

package workspace.plugins;

import java.io.File;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.ModuleTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import json.JSONArray;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.LSPWorkspaceManager;

public class TCPluginSL extends TCPlugin
{
	private TCModuleList tcModuleList = null;
	
	public TCPluginSL(LSPWorkspaceManager manager)
	{
		super(manager);
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
		tcModuleList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(astModuleList);
		tcModuleList.combineDefaults();
		TypeChecker tc = new ModuleTypeChecker(tcModuleList);
		tc.typeCheck();
		
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
	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		JSONArray results = new JSONArray();
		
		if (tcModuleList != null)	// May be syntax errors
		{
			for (TCModule module: tcModuleList)
			{
				if (module.files.contains(file))
				{
					results.add(messages.symbolInformation(module.name.toString(),
							module.name.getLocation(), SymbolKind.Module, null));

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
			
			return new RPCMessageList(request, results);
		}

		return null;	// No symbols available
	}
}
