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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.patterns;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpressionList;
import com.fujitsu.vdmj.po.patterns.visitors.POPatternVisitor;
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
