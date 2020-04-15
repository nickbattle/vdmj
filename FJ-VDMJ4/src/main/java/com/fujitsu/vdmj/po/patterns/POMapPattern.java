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
import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.util.Utils;

public class POMapPattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final POMapletPatternList maplets;

	public POMapPattern(LexLocation location, POMapletPatternList maplets)
	{
		super(location);
		this.maplets = maplets;
	}

	@Override
	public String toString()
	{
		if (maplets.isEmpty())
		{
			return "{|->}";
		}
		else
		{
			return Utils.listToString("{", maplets, ", ", "}");
		}
	}

	@Override
	public POExpression getMatchingExpression()
	{
		POMapletExpressionList list = new POMapletExpressionList();

		for (POMapletPattern p: maplets)
		{
			list.add(p.getMapletExpression());
		}

		return new POMapEnumExpression(location, list, null, null);
	}

	@Override
	public int getLength()
	{
		return maplets.size();
	}

	@Override
	public PODefinitionList getAllDefinitions(TCType type)
	{
		PODefinitionList defs = new PODefinitionList();

		TCMapType map = type.getMap();

		if (!map.empty)
		{
    		for (POMapletPattern p: maplets)
    		{
    			defs.addAll(p.getDefinitions(map));
    		}
		}

		return defs;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		TCNameList list = new TCNameList();

		for (POMapletPattern p: maplets)
		{
			list.addAll(p.getVariableNames());
		}

		return list;
	}

	@Override
	public TCType getPossibleType()
	{
		TCTypeSet types = new TCTypeSet();
		
		for (POMapletPattern p: maplets)
		{
			types.add(p.getPossibleType());
		}
		
		return types.isEmpty() ? new TCMapType(location) : types.getType(location);
	}

	@Override
	public boolean isSimple()
	{
		for (POMapletPattern p: maplets)
		{
			if (!p.isSimple()) return false;
		}

		return true;
	}

	@Override
	public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapPattern(this, arg);
	}
}
