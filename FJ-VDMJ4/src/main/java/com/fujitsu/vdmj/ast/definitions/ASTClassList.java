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

package com.fujitsu.vdmj.ast.definitions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * A class for holding a list of ClassDefinitions.
 */
public class ASTClassList extends Vector<ASTClassDefinition>
{
	private static final long serialVersionUID = 1L;

	public ASTClassList()
	{
		super();
	}

	public ASTClassList(ASTClassDefinition definition)
	{
		add(definition);
	}

	public Set<File> getSourceFiles()
	{
		Set<File> files = new HashSet<File>();

		for (ASTClassDefinition def: this)
		{
			if (!(def instanceof ASTCPUClassDefinition ||
				  def instanceof ASTBUSClassDefinition))
			{
				files.add(def.location.file);
			}
		}

		return files;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (ASTClassDefinition c: this)
		{
			sb.append(c.toString());
			sb.append("\n");
		}

		return sb.toString();
	}
}
