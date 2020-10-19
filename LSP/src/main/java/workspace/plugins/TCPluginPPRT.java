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
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;

import json.JSONArray;
import lsp.textdocument.SymbolKind;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.WorkspaceManager;

public class TCPluginPPRT extends TCPlugin
{
	private TCClassList tcClassList = null;
	
	public TCPluginPPRT(WorkspaceManager manager)
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
		tcClassList = new TCClassList();
	}
	
	public boolean checkLoadedFiles(ASTClassList astClassList) throws Exception
	{
		tcClassList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(astClassList);
		TypeChecker tc = new ClassTypeChecker(tcClassList);
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

	public TCClassList getTCClasses()
	{
		return tcClassList;
	}
	
	@Override
	public RPCMessageList documentSymbols(RPCRequest request, File file)
	{
		JSONArray results = new JSONArray();
		
		if (tcClassList != null)	// May be syntax errors
		{
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					results.add(messages.symbolInformation(clazz.name.toString(),
							clazz.name.getLocation(), SymbolKind.Class, null));

					for (TCDefinition def: clazz.definitions)
					{
						for (TCDefinition indef: def.getDefinitions())
						{
							results.add(messages.symbolInformation(indef.name.getName() + ":" + indef.getType(),
									indef.location, SymbolKind.kindOf(indef), indef.location.module));
						}
					}
				}
			}
			
			return new RPCMessageList(request, results);
		}
		
		return null;	// No symbols available
	}
}
