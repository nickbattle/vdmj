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

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.expressions.POBooleanLiteralExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;

public class POBooleanPattern extends POPattern
{
	private static final long serialVersionUID = 1L;
	public final LexBooleanToken value;

	public POBooleanPattern(LexBooleanToken token)
	{
		super(token.location);
		this.value = token;
	}

	@Override
	public String toString()
	{
		return value.toString();
	}

	@Override
	public PODefinitionList getAllDefinitions(TCType type)
	{
		return new PODefinitionList();
	}

	@Override
	public TCType getPossibleType()
	{
		return new TCBooleanType(location);
	}

	@Override
	public POExpression getMatchingExpression()
	{
		return new POBooleanLiteralExpression(value);
	}

	@Override
	public <R, S> R apply(POPatternVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBooleanPattern(this, arg);
	}
}
