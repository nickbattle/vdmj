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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.ValueList;

/**
 * A class to hold a list of Definitions.
 */
@SuppressWarnings("serial")
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

	public INStatement findStatement(int lineno)
	{
   		for (INDefinition d: this)
		{
			INStatement found = d.findStatement(lineno);

			if (found != null)
			{
				return found;
			}
		}

   		return null;
	}

	public INExpression findExpression(int lineno)
	{
   		for (INDefinition d: this)
		{
			INExpression found = d.findExpression(lineno);

			if (found != null)
			{
				return found;
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

	public ValueList getValues(Context ctxt)
	{
		ValueList list = new ValueList();

		for (INDefinition d: this)
		{
			list.addAll(d.getValues(ctxt));
		}

		return list;
	}

	public TCNameList getOldNames()
	{
		TCNameList list = new TCNameList();

		for (INDefinition d: this)
		{
			list.addAll(d.getOldNames());
		}

		return list;
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
