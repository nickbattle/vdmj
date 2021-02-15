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

package com.fujitsu.vdmj.in.definitions;

import java.math.BigInteger;

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.ast.lex.LexKeywordToken;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INEqualsExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INHistoryExpression;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.util.Utils;

public class INMutexSyncDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameList operations;

	public INMutexSyncDefinition(INAnnotationList annotations, LexLocation location, TCNameList operations)
	{
		super(location, null, null);
		this.annotations = annotations;
		this.operations = operations;
	}

	@Override
	public TCType getType()
	{
		return new TCUnknownType(location);
	}

	@Override
	public String toString()
	{
		return "mutex(" +
			(operations.isEmpty() ? "all)" :
				Utils.listToString("", operations, ", ", ")"));
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof INMutexSyncDefinition)
		{
			return toString().equals(other.toString());
		}
		
		return false;
	}
	
	@Override
	public int hashCode()
	{
		return toString().hashCode();
	}

	public INExpression getExpression(TCNameToken excluding)
	{
		TCNameList list = null;

		if (operations.size() == 1)
		{
			list = operations;
		}
		else
		{
			list = new TCNameList();
			list.addAll(operations);
			list.remove(excluding);
		}

		return new INEqualsExpression(
			new INHistoryExpression(location, Token.ACTIVE, list),
    		new LexKeywordToken(Token.EQUALS, location),
    		new INIntegerLiteralExpression(new LexIntegerToken(BigInteger.ZERO, location)));
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseMutexSyncDefinition(this, arg);
	}
}
