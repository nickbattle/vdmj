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

import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCRecordPattern extends TCPattern
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken typename;
	public final TCPatternList plist;
	public TCType type;

	public TCRecordPattern(TCNameToken typename, TCPatternList list)
	{
		super(typename.getLocation());
		this.plist = list;
		this.typename = typename;
		this.type = new TCUnresolvedType(typename);
	}

	@Override
	public String toString()
	{
		return "mk_" + type + "(" + Utils.listToString(plist) + ")";
	}

	@Override
	public void unResolve()
	{
		type.unResolve();
		resolved = false;
	}

	@Override
	public void typeResolve(Environment env)
	{
		if (resolved) return; else { resolved = true; }

		try
		{
			plist.typeResolve(env);
			type = type.typeResolve(env, null);
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	@Override
	public TCType getPossibleType()
	{
		return type;
	}

	@Override
	public boolean alwaysMatches()
	{
		return plist.alwaysMatches();
	}

	@Override
	public <R, S> R apply(TCPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseRecordPattern(this, arg);
	}
}
