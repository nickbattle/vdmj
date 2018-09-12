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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCTuplePattern extends TCPattern
{
	private static final long serialVersionUID = 1L;
	public final TCPatternList plist;

	public TCTuplePattern(LexLocation location, TCPatternList list)
	{
		super(location);
		this.plist = list;
	}

	@Override
	public String toString()
	{
		return "mk_" + "(" + Utils.listToString(plist) + ")";
	}

	@Override
	public void unResolve()
	{
		plist.unResolve();
		resolved = false;
	}

	@Override
	public void typeResolve(Environment env)
	{
		if (resolved) return; else { resolved = true; }

		try
		{
			plist.typeResolve(env);
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	@Override
	public TCDefinitionList getAllDefinitions(TCType type, NameScope scope)
	{
		TCDefinitionList defs = new TCDefinitionList();

		if (!type.isProduct(plist.size(), location))
		{
			report(3205, "Matching expression is not a product of cardinality " + plist.size());
			detail("Actual", type);
			return defs;
		}

		TCProductType product = type.getProduct(plist.size());
		Iterator<TCType> ti = product.types.iterator();

		for (TCPattern p: plist)
		{
			defs.addAll(p.getAllDefinitions(ti.next(), scope));
		}

		return defs;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		TCNameList list = new TCNameList();

		for (TCPattern p: plist)
		{
			list.addAll(p.getAllVariableNames());
		}

		return list;
	}

	@Override
	public TCType getPossibleType()
	{
		TCTypeList list = new TCTypeList();

		for (TCPattern p: plist)
		{
			list.add(p.getPossibleType());
		}

		return list.getType(location);
	}

	@Override
	public List<TCIdentifierPattern> findIdentifiers()
	{
		List<TCIdentifierPattern> list = new Vector<TCIdentifierPattern>();

		for (TCPattern p: plist)
		{
			list.addAll(p.findIdentifiers());
		}

		return list;
	}

	public boolean alwaysMatches()
	{
		return plist.alwaysMatches();
	}
}
