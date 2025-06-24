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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.NameValuePairList;

/**
 * A class to hold a list of Definitions.
 */
public class INDefinitionList extends INMappedList<TCDefinition, INDefinition>
{
	public INDefinitionList(TCDefinitionList from) throws Exception
	{
		super(from);
	}

	public INDefinitionList()
	{
		super();
	}

	public INDefinitionList(INDefinition definition)
	{
		add(definition);
	}

	public INStateDefinition findStateDefinition()
	{
   		for (INDefinition d: this)
		{
			if (d instanceof INStateDefinition)
			{
				return (INStateDefinition)d;
			}
		}

   		return null;
	}
	
	public INDefinition findName(TCNameToken name)
	{
		for (INDefinition d: this)
		{
			INDefinition def = d.findName(name);

			if (def != null)
			{
				return def;
			}
		}

		return null;
	}

	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList nvl = new NameValuePairList();

		for (INDefinition d: this)
		{
			nvl.addAll(d.getNamedValues(ctxt));
		}

		return nvl;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (INDefinition d: this)
		{
			if (d.isSubclassResponsibility())
			{
				sb.append("abstract ");
			}

			sb.append(d.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public boolean hasSubclassResponsibility()
	{
		for (INDefinition d: this)
		{
			if (d.isSubclassResponsibility())
			{
				return true;
			}
		}

		return false;
	}
}
