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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCParameterType extends TCType
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	
	private TCDefinition paramdef = null;
	public TCType paramPattern = null;

	public TCParameterType(TCNameToken pname)
	{
		super(pname.getLocation());
		this.name = pname;
		this.paramPattern = new TCUnknownType(location);
	}

	@Override
	public TCType typeResolve(Environment env)
	{
		if (resolved) return this; else resolved = true;

		paramdef = env.findName(name, NameScope.NAMES);

		if (paramdef == null || !(paramdef.getType() instanceof TCParameterType))
		{
			report(3433, "Parameter type @" + name + " not defined");
		}

		return paramdef.getType();	// Pick up @TypeParam pattern
	}
	
	public TCDefinition getDefinition()
	{
		return paramdef;	// Note that this is only set after typeRsolution
	}
	
	@Override
	public boolean isOrdered(LexLocation loc)
	{
		return paramPattern.isOrdered(loc);
	}

	@Override
	public boolean isUnion(LexLocation from)
	{
		return paramPattern.isUnion(location);
	}

	@Override
	public boolean isSeq(LexLocation from)
	{
		return paramPattern.isSeq(location);
	}

	@Override
	public boolean isSet(LexLocation from)
	{
		return paramPattern.isSet(location);
	}

	@Override
	public boolean isMap(LexLocation from)
	{
		return paramPattern.isMap(location);
	}

	@Override
	public boolean isRecord(LexLocation from)
	{
		return paramPattern.isRecord(from);
	}

	@Override
	public boolean isTag()
	{
		return paramPattern.isTag();
	}

	@Override
	public boolean isNumeric(LexLocation from)
	{
		return paramPattern.isNumeric(location);
	}

	@Override
	public boolean isProduct(LexLocation from)
	{
		return paramPattern.isProduct(location);
	}

	@Override
	public boolean isProduct(int n, LexLocation from)
	{
		return paramPattern.isProduct(n, location);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public String toDisplay()
	{
		return "@" + name;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCParameterType)
		{
			TCParameterType oparam = (TCParameterType)other;
			return oparam.name.equals(name);
		}
		
		return false;
	}

	@Override
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseParameterType(this, arg);
	}
}
