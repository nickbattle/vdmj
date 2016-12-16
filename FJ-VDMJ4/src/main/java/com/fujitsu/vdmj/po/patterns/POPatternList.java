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
import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.util.Utils;

@SuppressWarnings("serial")
public class POPatternList extends POMappedList<TCPattern, POPattern>
{
	public POPatternList(TCPatternList from) throws Exception
	{
		super(from);
	}
	
	public POPatternList()
	{
	}

	@Override
	public String toString()
	{
		return Utils.listToString(this);
	}

	public TCType getPossibleType(LexLocation location)
	{
		switch (size())
		{
			case 0:
				return new TCUnknownType(location);

			case 1:
				return get(0).getPossibleType();

			default:
        		TCTypeSet list = new TCTypeSet();

        		for (POPattern p: this)
        		{
        			list.add(p.getPossibleType());
        		}

        		return list.getType(location);		// NB. a union of types
		}
	}

	public POExpressionList getMatchingExpressionList()
	{
		POExpressionList list = new POExpressionList();

		for (POPattern p: this)
		{
			list.add(p.getMatchingExpression());
		}

		return list;
	}

	public boolean isSimple()
	{
		for (POPattern p: this)
		{
			if (!p.isSimple()) return false;		// NB. AND
		}

		return true;
	}

	public boolean alwaysMatches()
	{
		for (POPattern p: this)
		{
			if (!p.alwaysMatches()) return false;	// NB. AND
		}

		return true;
	}
}
