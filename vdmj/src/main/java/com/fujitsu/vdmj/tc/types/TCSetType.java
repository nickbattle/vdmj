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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCAccessSpecifier;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeCheckException;

public class TCSetType extends TCType
{
	private static final long serialVersionUID = 1L;
	public TCType setof;
	public final boolean empty;

	public TCSetType(LexLocation location, TCType setof, boolean empty)
	{
		super(location);
		this.setof = setof;
		this.empty = empty;
	}

	public TCSetType(LexLocation location, TCType setof)
	{
		super(location);
		this.setof = setof;
		this.empty = false;
	}

	public TCSetType(LexLocation location)
	{
		super(location);
		this.setof = new TCUnknownType(location);
		this.empty = true;
	}

	@Override
	public boolean narrowerThan(TCAccessSpecifier accessSpecifier)
	{
		return setof.narrowerThan(accessSpecifier);
	}

	@Override
	public boolean isSet(LexLocation from)
	{
		return true;
	}

	@Override
	public TCSetType getSet()
	{
		return this;
	}

	@Override
	public void unResolve()
	{
		if (!resolved) return; else { resolved = false; }
		setof.unResolve();
	}

	@Override
	public TCType typeResolve(Environment env)
	{
		if (resolved) return this; else { resolved = true; }

		try
		{
			setof = setof.typeResolve(env);
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
		return empty ? "set of ?" : "set of (" + setof + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other.getClass().equals(TCSetType.class))
		{
			TCSetType os = (TCSetType)other;
			// NB empty set same type as any set
			return empty || os.empty || setof.equals(os.setof);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return empty ? 0 : setof.hashCode();
	}

	@Override
	public TCTypeList getComposeTypes()
	{
		return setof.getComposeTypes();
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSetType(this, arg);
	}
}
