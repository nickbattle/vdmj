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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCMapPattern extends TCPattern
{
	private static final long serialVersionUID = 1L;
	public final TCMapletPatternList maplets;

	public TCMapPattern(LexLocation location, TCMapletPatternList maplets)
	{
		super(location);
		this.maplets = maplets;
	}

	@Override
	public void unResolve()
	{
		for (TCMapletPattern mp: maplets)
		{
			mp.unResolve();
		}

		resolved = false;
	}

	@Override
	public void typeResolve(Environment env)
	{
		if (resolved) return; else { resolved = true; }

		try
		{
			for (TCMapletPattern mp: maplets)
			{
				mp.typeResolve(env);
			}
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
		if (maplets.isEmpty())
		{
			return "{|->}";
		}
		else
		{
			return Utils.listToString("{", maplets, ", ", "}");
		}
	}

	@Override
	public int getLength()
	{
		return maplets.size();
	}

	@Override
	public TCDefinitionList getAllDefinitions(TCType type, NameScope scope)
	{
		TCDefinitionList defs = new TCDefinitionList();

		if (!type.isMap(location))
		{
			report(3314, "Map pattern is not matched against map type");
			detail("Actual type", type);
		}
		else
		{
			TCMapType map = type.getMap();

			if (!map.empty)
			{
        		for (TCMapletPattern p: maplets)
        		{
        			defs.addAll(p.getDefinitions(map, scope));
        		}
			}
		}

		return defs;
	}

	@Override
	public TCType getPossibleType()
	{
		TCTypeSet types = new TCTypeSet();
		
		for (TCMapletPattern p: maplets)
		{
			types.add(p.getPossibleType());
		}
		
		return types.isEmpty() ? new TCMapType(location) : types.getType(location);
	}

	@Override
	public List<TCIdentifierPattern> findIdentifiers()
	{
		List<TCIdentifierPattern> list = new Vector<TCIdentifierPattern>();

		for (TCMapletPattern p: maplets)
		{
			list.addAll(p.findIdentifiers());
		}

		return list;
	}

	@Override
	public <R, S> R apply(TCPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapPattern(this, arg);
	}
}
