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

package com.fujitsu.vdmj.ast.modules;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class ASTModuleList extends Vector<ASTModule> implements Mappable
{
	public ASTModuleList()
	{
		// empty
	}

	public ASTModuleList(ASTModuleList modules)
	{
		addAll(modules);
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public Set<File> getSourceFiles()
	{
		Set<File> files = new HashSet<File>();

		for (ASTModule def: this)
		{
			files.addAll(def.files);
		}

		return files;
	}
	
	public Set<String> getModuleNames()
	{
		Set<String> names = new HashSet<String>();
		
		for (ASTModule def: this)
		{
			names.add(def.name.name);
		}

		return names;
	}
}
