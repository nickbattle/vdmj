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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace.plugins;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.typechecker.ClassTypeChecker;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.util.DependencyOrder;

import json.JSONArray;
import json.JSONObject;
import lsp.textdocument.SymbolKind;
import vdmj.LSPDefinitionFinder;
import workspace.events.CheckPrepareEvent;
import workspace.events.CheckTypeEvent;
import workspace.lenses.TCCodeLens;

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
	protected void preCheck(CheckPrepareEvent ev)
	{
		super.preCheck(ev);
		tcClassList = new TCClassList();
	}
	
	@Override
	public <T extends Mappable> void checkLoadedFiles(T astClassList, CheckTypeEvent event) throws Exception
	{
		try
		{
			tcClassList = ClassMapper.getInstance(TCNode.MAPPINGS).init().convert(astClassList);
			TypeChecker tc = new ClassTypeChecker(tcClassList);
			tc.typeCheck();
		}
		catch (TypeCheckException te)
		{
			TypeChecker.report(3427, te.getMessage(), te.location);
		}
		catch (Throwable e)
		{
			TypeChecker.report(3430, e.toString(), LexLocation.ANY);
		}
		
		if (TypeChecker.getErrorCount() > 0)
		{
			messagehub.addPluginMessages(this, TypeChecker.getErrors());
		}
		
		if (TypeChecker.getWarningCount() > 0)
		{
			messagehub.addPluginMessages(this, TypeChecker.getWarnings());
		}
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
	public TCDefinition findDefinition(File file, long zline, long zcol)
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
	public JSONArray getCodeLenses(File file)
	{
		JSONArray results = new JSONArray();
		ASTPlugin ast = registry.getPlugin("AST");
		
		if (!tcClassList.isEmpty())	// May be syntax errors
		{
			List<TCCodeLens> lenses = getTCCodeLenses(ast.isDirty());
			
			for (TCClassDefinition clazz: tcClassList)
			{
				if (clazz.name.getLocation().file.equals(file))
				{
					for (TCDefinition def: clazz.definitions)
					{
						if (def.location.file.equals(file))
						{
							for (TCCodeLens lens: lenses)
							{
								results.addAll(lens.getDefinitionLenses(def, clazz));
							}
						}
					}
				}
			}
		}
		
		return results;
	}

	@Override
	public TCClassList getTypeHierarchy(String classname, boolean subtypes)
	{
		if (tcClassList != null)
		{
			TCClassDefinition cdef = find(classname);
			
			if (cdef == null)
			{
				return null;
			}
			else if (subtypes)
			{
				return subtypes(cdef);
			}
			else
			{
				return supertypes(cdef);
			}
		}
		else
		{
			return null;
		}
	}
	
	private TCClassDefinition find(String classname)
	{
		for (TCClassDefinition cdef: tcClassList)
		{
			if (cdef.name.getName().equals(classname))
			{
				return cdef;
			}
		}
		
		return null;
	}

	private TCClassList subtypes(TCClassDefinition cdef)
	{
		TCClassList subs = new TCClassList();
		
		for (TCClassDefinition sdef: tcClassList)
		{
			if (sdef.superdefs.contains(cdef))
			{
				subs.add(sdef);
				// subs.addAll(subtypes(sdef));
			}
		}
		
		return subs;
	}

	private TCClassList supertypes(TCClassDefinition cdef)
	{
		TCClassList supers = new TCClassList();
		supers.addAll(cdef.superdefs);
		
//		for (TCClassDefinition sdef: cdef.superdefs)
//		{
//			supers.addAll(supertypes(sdef));
//		}
		
		return supers;
	}
}
