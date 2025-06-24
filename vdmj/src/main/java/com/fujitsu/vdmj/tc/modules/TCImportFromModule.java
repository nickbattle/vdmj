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

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCImportFromModule extends TCNode
{
	private static final long serialVersionUID = 1L;

	public final TCIdentifierToken name;
	public final TCImportList signatures;

	public TCImportFromModule(TCIdentifierToken name, TCImportList signatures)
	{
		this.name = name;
		this.signatures = signatures;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("from " + name + "\n");

		for (TCImport type: signatures)
		{
			if (type instanceof TCImportAll)
			{
				sb.append("import all\n");
			}
			else
			{
				sb.append(type.toString());
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	public TCDefinitionList getDefinitions(TCModule from)
	{
		TCDefinitionList defs = new TCDefinitionList();

		for (TCImport imp: signatures)
		{
			defs.addAll(imp.getDefinitions(from));
		}

		return defs;
	}

	public void typeCheck(Environment env)
	{
		for (TCImport imp: signatures)
		{
			imp.typeCheck(env);
		}
	}
}
