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

package com.fujitsu.vdmj.tc.definitions;

import java.util.TreeSet;

import com.fujitsu.vdmj.mapper.Mappable;

/**
 * A class to hold a set of Definitions with unique names.
 */

public class TCDefinitionSet extends TreeSet<TCDefinition> implements Mappable
{
	public TCDefinitionSet()
	{
		super();
	}

	public TCDefinitionSet(TCDefinition definition)
	{
		add(definition);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (TCDefinition d: this)
		{
			sb.append(d.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public TCDefinitionList asList()
	{
		TCDefinitionList list = new TCDefinitionList();
		list.addAll(this);
		return list;
	}
}
