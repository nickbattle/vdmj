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

import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POSetUnionExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;

public class POUnionPattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final POPattern left;
	public final POPattern right;

	public POUnionPattern(POPattern left, LexLocation location, POPattern right)
	{
		super(location);
		this.left = left;
		this.right = right;
	}

	@Override
	public String toString()
	{
		return left + " union " + right;
	}

	@Override
	public POExpression getMatchingExpression()
	{
		LexToken op = new LexKeywordToken(Token.UNION, location);
		return new POSetUnionExpression(
			left.getMatchingExpression(), op, right.getMatchingExpression(), null, null);
	}

	@Override
	public int getLength()
	{
		int llen = left.getLength();
		int rlen = right.getLength();
		return (llen == ANY || rlen == ANY) ? ANY : llen + rlen;
	}

	@Override
	public PODefinitionList getAllDefinitions(TCType type)
	{
		PODefinitionList defs = new PODefinitionList();

		defs.addAll(left.getAllDefinitions(type));
		defs.addAll(right.getAllDefinitions(type));

		return defs;
	}

	@Override
	public TCNameList getAllVariableNames()
	{
		TCNameList list = new TCNameList();

		list.addAll(left.getAllVariableNames());
		list.addAll(right.getAllVariableNames());

		return list;
	}

	@Override
	public TCType getPossibleType()
	{
		TCTypeSet list = new TCTypeSet();

		list.add(left.getPossibleType());
		list.add(right.getPossibleType());

		TCType s = list.getType(location);

		return s.isUnknown(location) ?
			new TCSetType(location, new TCUnknownType(location)) : s;
	}

	@Override
	public boolean isSimple()
	{
		return left.isSimple() && right.isSimple();
	}

	public boolean alwaysMatches()
	{
		return left.alwaysMatches() && right.alwaysMatches();
	}
}
