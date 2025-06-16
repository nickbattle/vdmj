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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.lex.LexLocation;

public class INPatternBind extends INNode
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final INPattern pattern;
	public final INBind bind;

	public INPatternBind(LexLocation location, INPattern pattern, INBind bind)
	{
		this.location = location;
		this.pattern = pattern;
		this.bind = bind;
	}

	@Override
	public String toString()
	{
		return (pattern == null ? bind : pattern).toString();
	}
}
