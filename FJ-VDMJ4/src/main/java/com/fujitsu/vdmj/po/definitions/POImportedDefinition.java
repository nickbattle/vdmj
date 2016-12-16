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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold an imported definition.
 */
public class POImportedDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final PODefinition def;

	public POImportedDefinition(LexLocation location, PODefinition def)
	{
		super(location, def.name);
		this.def = def;
	}

	@Override
	public String toString()
	{
		return def.toString();
	}

	@Override
	public TCType getType()
	{
		return def.getType();
	}

	@Override
	public TCNameList getVariableNames()
	{
		return def.getVariableNames();
	}
}
