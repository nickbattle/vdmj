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

package com.fujitsu.vdmj.po.patterns;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;

public class POPatternBind implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final LexLocation location;
	public final POPattern pattern;
	public final POBind bind;
	public final PODefinitionList defs;

	public POPatternBind(LexLocation location, POPattern pattern, POBind bind, PODefinitionList defs)
	{
		this.location = location;
		this.pattern = pattern;
		this.bind = bind;
		this.defs = defs;
	}

	@Override
	public String toString()
	{
		return (pattern == null ? bind : pattern).toString();
	}

	public PODefinitionList getDefinitions()
	{
		return defs;
	}
}
