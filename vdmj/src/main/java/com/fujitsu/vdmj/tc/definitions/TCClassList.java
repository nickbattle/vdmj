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

package com.fujitsu.vdmj.tc.definitions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class for holding a list of ClassDefinitions.
 */
public class TCClassList extends TCMappedList<ASTClassDefinition, TCClassDefinition>
{
	private static final long serialVersionUID = 1L;

	public TCClassList()
	{
		super();
	}

	public TCClassList(ASTClassList from) throws Exception
	{
		super(from);
	}

	public TCClassList(TCClassDefinition definition)
	{
		add(definition);
	}

	public Set<File> getSourceFiles()
	{
		Set<File> files = new HashSet<File>();

		for (TCClassDefinition def: this)
		{
			if (!(def instanceof TCCPUClassDefinition ||
				  def instanceof TCBUSClassDefinition))
			{
				files.add(def.location.file);
			}
		}

		return files;
	}

	public void implicitDefinitions(Environment env)
	{
		for (TCClassDefinition d: this)
		{
			d.implicitDefinitions(env);
		}
	}

	public void unusedCheck()
	{
		for (TCClassDefinition d: this)
		{
			d.unusedCheck();
		}
	}

	public TCDefinition findName(TCNameToken name, NameScope scope)
	{
		for (TCClassDefinition d: this)
		{
			TCDefinition def = d.findName(name, scope);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	public TCDefinition findType(TCNameToken name)
	{
		for (TCClassDefinition d: this)
		{
			TCDefinition def = d.findType(name, null);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	public TCDefinitionSet findMatches(TCNameToken name)
	{
		TCDefinitionSet set = new TCDefinitionSet();

		for (TCClassDefinition d: this)
		{
			set.addAll(d.findMatches(name));
		}

		return set;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (TCClassDefinition c: this)
		{
			sb.append(c.toString());
			sb.append("\n");
		}

		return sb.toString();
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
		for (TCClassDefinition clazz: this)
		{
			for (TCDefinition def: clazz.definitions)
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
