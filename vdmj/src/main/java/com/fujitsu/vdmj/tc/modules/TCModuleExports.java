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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;

public class TCModuleExports extends TCNode
{
	private static final long serialVersionUID = 1L;
	public final TCExportList exports;

	public TCModuleExports(TCExportList exports)
	{
		this.exports = exports;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (TCExport type: exports)
		{
			sb.append(type.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public TCDefinitionList getDefinitions()
	{
		TCDefinitionList exportDefs = new TCDefinitionList();

		for (TCExport exp: exports)
		{
			exportDefs.addAll(exp.getDefinition());
		}

		return exportDefs;
	}

	public TCDefinitionList getDefinitions(TCDefinitionList actualDefs)
	{
		TCDefinitionList exportDefs = new TCDefinitionList();

		for (TCExport exp: exports)
		{
			exportDefs.addAll(exp.getDefinition(actualDefs));
		}

		// Mark all exports as used

		for (TCDefinition d: exportDefs)
		{
			d.markUsed();
		}

		return exportDefs;
	}

	public void typeCheck(ModuleEnvironment env, TCDefinitionList actualDefs)
	{
		for (TCExport exp: exports)
		{
			exp.typeCheck(env, actualDefs);
		}
	}
}
