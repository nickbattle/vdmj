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

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

public class TCTraceVariable extends TCNode implements Comparable<TCTraceVariable>
{
	private static final long serialVersionUID = 1L;

	public final TCNameToken name;
	public final Object value;
	public final TCType type;
	public final boolean clone;
	
	private final String cached;

	public TCTraceVariable(TCNameToken name, Object value, TCType tcType, boolean clone)
	{
		this.name = name;
		this.value = value;
		this.type = tcType;
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
	public int compareTo(TCTraceVariable o)
	{
		return toString().compareTo(o.toString());
	}
}
