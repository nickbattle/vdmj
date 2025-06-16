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

import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCPatternTypePair extends TCNode
{
	private static final long serialVersionUID = 1L;
	public final TCPattern pattern;
	public TCType type;
	private boolean resolved = false;

	public TCPatternTypePair(TCPattern pattern, TCType type)
	{
		this.pattern = pattern;
		this.type = type;
	}

	public void typeResolve(Environment base)
	{
		if (resolved ) return; else { resolved = true; }
		type = type.typeResolve(base);
	}

	public TCDefinitionList getDefinitions()
	{
		return pattern.getDefinitions(type, NameScope.LOCAL);
	}

	@Override
	public String toString()
	{
		return pattern + ":" + type;
	}
}
