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

package com.fujitsu.vdmj.in.traces;

import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.Value;

public class INTraceVariable implements Comparable<INTraceVariable>
{
	public final TCNameToken name;
	public final Value value;
	public final TCType type;
	public final boolean clone;
	
	private final String cached;

	public INTraceVariable(TCNameToken name, Value value, TCType type, boolean clone)
	{
		this.name = name;
		this.value = value;
		this.type = type;
		this.clone = clone;
		this.cached = "(" + name + " = " + value + ")";
	}

	@Override
	public String toString()
	{
		return cached;
	}
	
	@Override
	public boolean equals(Object other)
	{
		return toString().equals(other.toString());
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	@Override
	public int compareTo(INTraceVariable o)
	{
		return toString().compareTo(o.toString());
	}
}
