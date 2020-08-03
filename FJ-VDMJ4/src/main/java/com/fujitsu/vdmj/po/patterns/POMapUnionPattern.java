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
import com.fujitsu.vdmj.po.expressions.POMapUnionExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POPatternVisitor;
import com.fujitsu.vdmj.tc.types.TCType;


public class POMapUnionPattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final POPattern left;
	public final POPattern right;

	public POMapUnionPattern(POPattern left, LexLocation location, POPattern right)
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
		LexToken op = new LexKeywordToken(Token.MUNION, location);
		return new POMapUnionExpression(
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
	public boolean isSimple()
	{
		return left.isSimple() && right.isSimple();
	}

	@Override
	public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMapUnionPattern(this, arg);
	}
}
