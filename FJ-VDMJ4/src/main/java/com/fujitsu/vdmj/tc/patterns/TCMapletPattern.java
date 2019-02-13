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

package com.fujitsu.vdmj.tc.patterns;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCMapletPattern extends TCNode
{
	private static final long serialVersionUID = 1L;
	public final TCPattern from;
	public final TCPattern to;
	private boolean resolved = false;

	public TCMapletPattern(TCPattern from, TCPattern to)
	{
		this.from = from;
		this.to = to;
	}

	public void unResolve()
	{
		from.unResolve();
		to.unResolve();
		resolved = false;
	}

	public void typeResolve(Environment env)
	{
		if (resolved) return; else { resolved = true; }

		try
		{
			from.typeResolve(env);
			to.typeResolve(env);
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	@Override
	public String toString()
	{
		return from + " |-> " + to;
	}

	public TCDefinitionList getDefinitions(TCMapType map, NameScope scope)
	{
		TCDefinitionList list = new TCDefinitionList();
		list.addAll(from.getAllDefinitions(map.from, scope));
		list.addAll(to.getAllDefinitions(map.to, scope));
		return list;
	}

	public TCNameList getVariableNames()
	{
		TCNameList list = new TCNameList();

		list.addAll(from.getAllVariableNames());
		list.addAll(to.getAllVariableNames());

		return list;
	}

	public List<TCIdentifierPattern> findIdentifiers()
	{
		List<TCIdentifierPattern> list = new Vector<TCIdentifierPattern>();

		list.addAll(from.findIdentifiers());
		list.addAll(to.findIdentifiers());

		return list;
	}
	
	public TCType getPossibleType()
	{
		return new TCMapType(from.location, from.getPossibleType(), to.getPossibleType());
	}
}
