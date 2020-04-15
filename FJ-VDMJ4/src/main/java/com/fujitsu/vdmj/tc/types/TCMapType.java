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
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCMapType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCType from;
	public TCType to;
	public final boolean empty;

	public TCMapType(LexLocation location, TCType from, TCType to)
	{
		super(location);
		this.from = from;
		this.to = to;
		this.empty = false;
	}

	public TCMapType(LexLocation location)
	{
		super(location);
		this.from = new TCUnknownType(location);
		this.to = new TCUnknownType(location);
		this.empty = true;
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		return from.narrowerThan(accessSpecifier) ||
				to.narrowerThan(accessSpecifier);
	}

	@Override
	public boolean isMap(LexLocation from)
	{
		return true;
	}

	@Override
	public TCMapType getMap()
	{
		return this;
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }

		if (!empty)
		{
			from.unResolve();
			to.unResolve();
		}
	}

	@Override
	public TCMapType typeResolve(Environment env, TCTypeDefinition root)
	{
		if (resolved) return this; else { resolved = true; }

		try
		{
			if (!empty)
			{
				from = from.typeResolve(env, root);
				to = to.typeResolve(env, root);
			}

			return this;
		}
		catch (TypeCheckException e)
		{
			unResolve();
			throw e;
		}
	}

	@Override
	public String toDisplay()
	{
		return "map (" + from + ") to (" + to + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other.getClass() == getClass())	// inmaps too
		{
			TCMapType mt = (TCMapType)other;
			return from.equals(mt.from) && to.equals(mt.to);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return from.hashCode() + to.hashCode();
	}
	
	@Override
	public TCTypeList getComposeTypes()
	{
		TCTypeList list = new TCTypeList();
		list.addAll(from.getComposeTypes());
		list.addAll(to.getComposeTypes());
		return list;
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapType(this, arg);
	}

	@Override
	public TCType clone()
	{
		return new TCMapType(location, from.clone(), to.clone());
	}
}
