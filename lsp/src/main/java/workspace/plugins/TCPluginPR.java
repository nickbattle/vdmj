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
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.util.DependencyOrder;

import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import lsp.textdocument.SymbolKind;
import vdmj.LSPDefinitionFinder;
import workspace.LSPWorkspaceManager;

public class TCPluginPR extends TCPlugin
{
	private TCClassList tcClassList = null;
	
	public TCPluginPR()
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
		tcClassList = new TCClassList();
	}
	
	@Override
	public <T extends Mappable> boolean checkLoadedFiles(T astClassList) throws Exception
	{
		try
		{
			tcClassList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(astClassList);
			TypeChecker tc = new ClassTypeChecker(tcClassList);
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
	public <T extends Mappable> T getTC()
	{
		return (T)tcClassList;
	}
	
	@Override
	public JSONArray documentSymbols(File file)
	{
		JSONArray results = new JSONArray();
		
		if (!tcClassList.isEmpty())	// May be syntax errors
		{
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					results.add(documentSymbols(clazz));
				}
			}
		}
		
		return results;
	}
	
	private JSONObject documentSymbols(TCClassDefinition clazz)
	{
		JSONArray symbols = new JSONArray();

		for (TCDefinition def: clazz.definitions)
		{
			JSONObject symbol = documentSymbolsTop(def);
			if (symbol != null) symbols.add(symbol);
		}

		return messages.documentSymbol(
			clazz.name.getName(),
			"",
			SymbolKind.Class,
			LexLocation.getSpan(clazz.name.getLex()),
			clazz.name.getLocation(),
			symbols);
	}

	@Override
	public TCDefinition findDefinition(File file, int zline, int zcol)
	{
		if (tcClassList != null && !tcClassList.isEmpty())
		{
			LSPDefinitionFinder finder = new LSPDefinitionFinder();
			return finder.findDefinition(tcClassList, file, zline + 1, zcol + 1);		// Convert from zero-relative
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
		
		for (TCClassDefinition cdef: tcClassList)
		{
			if (cdef.name.getName().startsWith(startsWith))
			{
				results.add(cdef);	// Add classes as well

				for (TCDefinition def: cdef.definitions.singleDefinitions())
				{
					if (def.name != null)
					{
						results.add(def);
					}
				}
			}
			
			for (TCDefinition def: cdef.definitions.singleDefinitions())
			{
				if (def.name != null && def.name.getName().startsWith(startsWith))
				{
					results.add(def);
				}
			}
		}
		
		return results;
	}

	@Override
	public void saveDependencies(File saveUri) throws IOException
	{
		if (tcClassList != null)
		{
			DependencyOrder order = new DependencyOrder();
			order.classOrder(tcClassList);
			order.graphOf(saveUri);
		}
	}

	@Override
	public JSONArray documentLenses(File file)
	{
		JSONArray results = new JSONArray();
		
		if (!tcClassList.isEmpty())	// May be syntax errors
		{
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					for (TCDefinition def: clazz.definitions)
					{
						if (def.location.file.equals(file))
						{
							results.addAll(launchDebugLensVSCode(def));
							// etc for other lenses...
						}
					}
				}
			}
		}
		
		return results;
	}
	
	/**
	 * Note this lens is VSCode specific, because it includes a VSCode command.
	 */
	private JSONArray launchDebugLensVSCode(TCDefinition def)
	{
		JSONArray results = new JSONArray();
		String name = LSPWorkspaceManager.getInstance().getClientInfo("name");
		
		if ("vscode".equals(name))
		{
			if (def.isCallableFunction() || def.isCallableOperation())
			{
				if (def.accessSpecifier.access == Token.PUBLIC)	// Not private or protected
				{
					results.add(
						new JSONObject(
							"range", Utils.lexLocationToRange(def.location),
							"command", new JSONObject(
									"title", "Launch",
									"command", CODE_LENS_COMMAND,
									"arguments", launchArgs(def, false))));
						
					results.add(
						new JSONObject(
							"range", Utils.lexLocationToRange(def.location),
							"command", new JSONObject(
									"title", "Debug",
									"command", CODE_LENS_COMMAND,
									"arguments", launchArgs(def, true))));
				}
			}
		}
		
		return results;
	}
}
