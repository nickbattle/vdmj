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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.typechecker.Environment;
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
	public <R, S> R apply(TCPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapPattern(this, arg);
	}
}
