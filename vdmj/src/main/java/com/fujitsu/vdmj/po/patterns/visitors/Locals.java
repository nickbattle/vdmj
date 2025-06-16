/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.po.patterns.visitors;

import java.util.Collection;

import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A nested list of local variables, used by POFreeVariableFinder.
 */
public class Locals
{
	public final Collection<TCNameToken> names;
	public final Locals outer;
	
	public Locals()
	{
		this.names = new TCNameSet();
		this.outer = null;
	}
	
	public Locals(Collection<TCNameToken> names, Locals outer)
	{
		this.names = names;
		this.outer = outer;
	}
	
	public boolean find(TCNameToken sought)
	{
		if (names.contains(sought))
		{
			return true;
		}
		
		return (outer == null) ? false : outer.find(sought);
	}
};

