/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.tc.modules;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.modules.ASTModuleList;
import com.fujitsu.vdmj.mapper.FileList;
import com.fujitsu.vdmj.syntax.ModuleReader;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class TCModuleList extends TCMappedList<ASTModule, TCModule>
{
	public TCModuleList()
	{
		super();
	}

	public TCModuleList(ASTModuleList from) throws Exception
	{
		super(from);
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public Set<File> getSourceFiles()
	{
		Set<File> files = new HashSet<File>();

		for (TCModule def: this)
		{
			files.addAll(def.files);
		}

		return files;
	}

	public TCModule findModule(TCIdentifierToken sought)
	{
		for (TCModule m: this)
		{
			if (m.name.equals(sought))
			{
				return m;
			}
		}

   		return null;
	}

	public int combineDefaults()
	{
		int rv = 0;

		if (!isEmpty())
		{
			TCModule def = new TCModule();

			if (Settings.release == Release.VDM_10)
			{
				// In VDM-10, we implicitly import all from the other
				// executableModules included with the flat specifications (if any).

    			TCImportFromModuleList imports = new TCImportFromModuleList();

    			for (TCModule m: this)
    			{
    				if (!m.isFlat)
    				{
    					imports.add(ModuleReader.importAll(m.name));
    				}
    			}

    			if (!imports.isEmpty())
    			{
    				def = new TCModule(null, def.name, new TCModuleImports(def.name, imports), null, def.defs, new FileList(), true);
    			}
			}

			TCModuleList named = new TCModuleList();

			for (TCModule m: this)
			{
				if (m.isFlat)
				{
					def.defs.addAll(m.defs);
					def.files.add(m.name.getLocation().file);
				}
				else
				{
					named.add(m);
				}
			}

			if (!def.defs.isEmpty())
			{
				clear();
				add(def);
				addAll(named);

				for (TCDefinition d: def.defs)
				{
					if (!d.isTypeDefinition())
					{
						d.markUsed();	// Mark top-level items as used
					}
				}
			}
		}

		return rv;
	}

	public TCDefinitionList findDefinitions(Stack<TCNameToken> stack)
	{
		TCDefinitionList list = new TCDefinitionList();
		
		for (TCNameToken name: stack)
		{
			list.add(findDefinition(name));
		}
		
		return list.contains(null) ? null : list;	// Usually local func definitions
	}

	private TCDefinition findDefinition(TCNameToken sought)
	{
		for (TCModule module: this)
		{
			for (TCDefinition def: module.defs)
			{
				if (def.name != null && def.name.equals(sought))
				{
					return def;
				}
			}
		}
		
		return null;
	}
}
