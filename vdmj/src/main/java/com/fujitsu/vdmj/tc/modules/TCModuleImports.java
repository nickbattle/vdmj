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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.modules;

import java.io.Serializable;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeChecker;

public class TCModuleImports extends TCNode
{
	private static final long serialVersionUID = 1L;
	public final TCIdentifierToken name;
	public final TCImportFromModuleList imports;

	public TCModuleImports(TCIdentifierToken name, TCImportFromModuleList imports)
	{
		this.name = name;
		this.imports = imports;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (TCImportFromModule def: imports)
		{
			sb.append(def.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public TCDefinitionList getDefinitions(TCModuleList allModules)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCImportFromModule ifm: imports)
		{
			if (ifm.name.equals(name))
			{
				TypeChecker.report(3195, "Cannot import from self", ifm.name.getLocation());
				continue;
			}

			TCModule from = allModules.findModule(ifm.name);

			if (from == null)
			{
				TypeChecker.report(3196, "No such module as " + ifm.name, ifm.name.getLocation());
			}
			else
			{
				defs.addAll(ifm.getDefinitions(from));
			}
		}

		return defs;
	}

	public void typeCheck(Environment env)
	{
		for (TCImportFromModule ifm: imports)
		{
			ifm.typeCheck(env);
		}
	}
}
