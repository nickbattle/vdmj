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
import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.FileList;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;
import com.fujitsu.vdmj.typechecker.TypeChecker;

/**
 * A class holding all the details for one module.
 */
public class TCModule extends TCNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The module name. */
	public final TCIdentifierToken name;
	/** A list of import declarations. */
	public final TCModuleImports imports;
	/** A list of export declarations. */
	public final TCModuleExports exports;
	/** A list of definitions created in the module. */
	public final TCDefinitionList defs;
	/** A list of source file names for the module. */
	public final FileList files;

	/** Those definitions which are exported. */
	public TCDefinitionList exportdefs;
	/** Definitions of imported objects from other executableModules. */
	public TCDefinitionList importdefs;
	/** True if the "module" is actually a flat definition file. */
	public boolean isFlat = false;

	/**
	 * Create a module from the given name and definitions.
	 */
	public TCModule(TCIdentifierToken name,
		TCModuleImports imports, TCModuleExports exports, TCDefinitionList defs, FileList files, boolean isFlat)
	{
		this.name = name;
		this.imports = imports;
		this.exports = exports;
		this.defs = defs;
		this.files = files;
		this.isFlat = isFlat;

		exportdefs = new TCDefinitionList();	// By default, export nothing
		importdefs = new TCDefinitionList();	// and import nothing
	}

	/**
	 * Create a module with a default name from the given definitions.
	 */
	public TCModule(File file, TCDefinitionList defs)
	{
		if (defs.isEmpty())
		{
			this.name =	defaultName(new LexLocation());
		}
		else
		{
    		this.name = defaultName(defs.get(0).location);
 		}

		this.imports = null;
		this.exports = null;
		this.defs = defs;
		this.files = new FileList();

		if (file != null)
		{
			files.add(file);
		}

		exportdefs = new TCDefinitionList();	// TCExport nothing
		importdefs = new TCDefinitionList();	// and import nothing

		isFlat = true;
	}

	/**
	 * Create a module called DEFAULT with no file and no definitions.
	 */

	public TCModule()
	{
		this(null, new TCDefinitionList());
	}

	/**
	 * Generate the default module name.
	 *
	 * @param location	The textual location of the name
	 * @return	The default module name.
	 */

	public static TCIdentifierToken defaultName(LexLocation location)
	{
		return new TCIdentifierToken(location, "DEFAULT", false);
	}

	/**
	 * Generate the exportdefs list of definitions. The exports list of
	 * export declarations is processed by searching the defs list of
	 * locally defined objects. The exportdefs field is populated with
	 * the result.
	 */

	public void processExports()
	{
		if (exports != null)
		{
			exportdefs.addAll(exports.getDefinitions(defs));
		}
	}

	/**
	 * Generate the importdefs list of definitions. The imports list of
	 * import declarations is processed by searching the module list passed
	 * in. The importdefs field is populated with the result.
	 */

	public void processImports(TCModuleList allModules)
	{
		if (imports != null)
		{
			TCDefinitionList updated = imports.getDefinitions(allModules);

			D: for (TCDefinition u: updated)
			{
				for (TCDefinition tc: importdefs)
				{
					if (tc.name != null && u.name != null && tc.name.matches(u.name))
					{
						u.used = tc.used;	// Copy usage from TC phase
						continue D;
					}
				}
			}

			importdefs.clear();
			importdefs.addAll(updated);
		}
	}

	/**
	 * TCType check the imports, compared to their export definitions.
	 */

	public void typeCheckImports()
	{
		if (imports != null)
		{
			imports.typeCheck(new ModuleEnvironment(this));
		}
	}

	/**
	 * TCType check the exports, compared to their local definitions.
	 */

	public void typeCheckExports()
	{
		if (exports != null)
		{
			exports.typeCheck(new ModuleEnvironment(this), defs);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("module " + name.getName() + "\n");

		if (imports != null)
		{
			sb.append("\nimports\n\n");
			sb.append(imports.toString());
		}

		if (exports != null)
		{
			sb.append("\nexports\n\n");
			sb.append(exports.toString());
		}
		else
		{
			sb.append("\nexports all\n\n");
		}

		if (defs != null)
		{
			sb.append("\ndefinitions\n\n");

			for (TCDefinition def: defs)
			{
				sb.append(def.toString() + "\n");
			}
		}

		sb.append("\nend " + name.getName() + "\n");

		return sb.toString();
	}

	public void checkOver()
	{
		List<String> done = new Vector<String>();

		TCDefinitionList singles = defs.singleDefinitions();

		for (TCDefinition def1: singles)
		{
			for (TCDefinition def2: singles)
			{
				if (def1 != def2 &&
					def1.name != null && def2.name != null &&
					def1.name.getName().equals(def2.name.getName()) &&
					!done.contains(def1.name.getName()))
				{
					if ((def1.isFunction() && !def2.isFunction()) ||
						(def1.isOperation() && !def2.isOperation()))
					{
						def1.report(3017, "Duplicate definitions for " + def1.name.getName());
						TypeChecker.detail2(def1.name.getName(), def1.location, def2.name.getName(), def2.location);
						done.add(def1.name.getName());
					}
				}
			}
		}
	}
}
