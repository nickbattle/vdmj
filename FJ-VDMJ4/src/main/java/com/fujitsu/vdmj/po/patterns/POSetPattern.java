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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POSetEnumExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;


public class POSetPattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final POPatternList plist;

	public POSetPattern(LexLocation location, POPatternList set)
	{
		super(location);
		this.plist = set;
	}

	@Override
	public String toString()
	{
		return "{" + plist.toString() + "}";
	}

	@Override
	public POExpression getMatchingExpression()
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: plist)
		{
			list.add(p.getMatchingExpression());
		}

		return new POSetEnumExpression(location, list, null);
	}

	@Override
	public int getLength()
	{
		return plist.size();
	}

	@Override
	public PODefinitionList getAllDefinitions(TCType type)
	{
		PODefinitionList defs = new PODefinitionList();

		TCSetType set = type.getSet();

		if (!set.empty)
		{
    		for (POPattern p: plist)
    		{
    			defs.addAll(p.getAllDefinitions(set.setof));
    		}
		}

		return defs;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		TCNameList list = new TCNameList();

		for (POPattern p: plist)
		{
			list.addAll(p.getAllVariableNames());
		}

		return list;
	}

	@Override
	public TCType getPossibleType()
	{
		return new TCSetType(location, plist.getPossibleType(location));
	}

	@Override
	public boolean isSimple()
	{
		return plist.isSimple();
	}
}
