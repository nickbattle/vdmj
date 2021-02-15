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
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POTupleExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POPatternVisitor;
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
	public boolean isSimple()
	{
		return plist.isSimple();
	}

	@Override
	public boolean alwaysMatches()
	{
		return plist.alwaysMatches();
	}

	@Override
	public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTuplePattern(this, arg);
	}
}
