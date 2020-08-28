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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.util.Utils;

public class TCProductType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCTypeList types = null;

	public TCProductType(LexLocation location, TCTypeList types)
	{
		super(location);
		this.types = types;
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		for (TCType t: types)
		{
			if (t.narrowerThan(accessSpecifier))
			{
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean isProduct(LexLocation from)
	{
		return true;
	}

	@Override
	public boolean isProduct(int n, LexLocation from)
	{
		return n == 0 || types.size() == n;
	}

	@Override
	public TCProductType getProduct()
	{
		return this;
	}

	@Override
	public TCProductType getProduct(int n)
	{
		return n == 0 || types.size() == n ? this : null;
	}

	@Override
	public void unResolve()
	{
		if (resolveErrors++ > MAX_RESOLVE_ERRORS) return;
		if (!resolved) return; else { resolved = false; }

		for (TCType t: types)
		{
			t.unResolve();
		}
	}

	@Override
	public TCProductType typeResolve(Environment env, TCTypeDefinition root)
	{
		if (resolved) return this; else { resolved = true; }

		TCTypeList fixed = new TCTypeList();
		TypeCheckException problem = null;

		for (TCType t: types)
		{
			try
			{
				TCType rt = t.typeResolve(env, root);
				fixed.add(rt);
			}
			catch (TypeCheckException e)
			{
				if (problem == null)
				{
					problem = e;
				}
				else
				{
					// Add extra messages to the exception for each product member
					problem.addExtra(e);
				}
			}
		}
		
		if (problem != null)
		{
			unResolve();
			throw problem;
		}

		types = fixed;
		return this;
	}

	@Override
	public String toDisplay()
	{
		return Utils.listToString("(", types, " * ", ")");
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof TCProductType)
		{
			TCProductType pother = (TCProductType)other;
			return this.types.equals(pother.types);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return types.hashCode();
	}
	
	@Override
	public TCTypeList getComposeTypes()
	{
		return types.getComposeTypes();
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseProductType(this, arg);
	}
}
