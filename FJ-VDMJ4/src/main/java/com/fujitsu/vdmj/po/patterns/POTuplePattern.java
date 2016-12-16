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

import java.util.Iterator;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POTupleExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.util.Utils;

public class POTuplePattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final POPatternList plist;

	public POTuplePattern(LexLocation location, POPatternList list)
	{
		super(location);
		this.plist = list;
	}

	@Override
	public String toString()
	{
		return "mk_" + "(" + Utils.listToString(plist) + ")";
	}

	@Override
	public POExpression getMatchingExpression()
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: plist)
		{
			list.add(p.getMatchingExpression());
		}

		return new POTupleExpression(location, list, null);
	}

	@Override
	public PODefinitionList getAllDefinitions(TCType type)
	{
		PODefinitionList defs = new PODefinitionList();
		TCProductType product = type.getProduct(plist.size());
		Iterator<TCType> ti = product.types.iterator();

		for (POPattern p: plist)
		{
			defs.addAll(p.getAllDefinitions(ti.next()));
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
		TCTypeList list = new TCTypeList();

		for (POPattern p: plist)
		{
			list.add(p.getPossibleType());
		}

		return list.getType(location);
	}

	@Override
	public boolean isSimple()
	{
		return plist.isSimple();
	}

	@Override
	public boolean alwaysMatches()
	{
		return plist.alwaysMatches();
	}
}
