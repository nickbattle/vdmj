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
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POMapletExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCType;

public class POMapletPattern extends PONode
{
	private static final long serialVersionUID = 1L;
	public final POPattern from;
	public final POPattern to;

	public POMapletPattern(POPattern from, POPattern to)
	{
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return from + " |-> " + to;
	}

	public POMapletExpression getMapletExpression()
	{
		LexToken op = new LexKeywordToken(Token.MAPLET, from.location);
		return new POMapletExpression(op.location, from.getMatchingExpression(), to.getMatchingExpression());
	}

	public PODefinitionList getDefinitions(TCMapType map)
	{
		PODefinitionList list = new PODefinitionList();
		list.addAll(from.getAllDefinitions(map.from));
		list.addAll(to.getAllDefinitions(map.to));
		return list;
	}

	public TCNameList getVariableNames()
	{
		TCNameList list = new TCNameList();

		list.addAll(from.getAllVariableNames());
		list.addAll(to.getAllVariableNames());

		return list;
	}

	public boolean isSimple()
	{
		return from.isSimple() && to.isSimple();
	}
	
	public TCType getPossibleType()
	{
		return new TCMapType(from.location, from.getPossibleType(), to.getPossibleType());
	}
}
