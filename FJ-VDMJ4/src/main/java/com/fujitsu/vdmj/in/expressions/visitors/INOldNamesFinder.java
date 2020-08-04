/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package com.fujitsu.vdmj.in.expressions.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class INOldNamesFinder extends INLeafExpressionVisitor<TCNameToken, TCNameList, Object>
{
	public INOldNamesFinder()
	{
		super(false);
		visitorSet = new INVisitorSet<TCNameToken, TCNameList, Object>() {};
	}

	@Override
	protected TCNameList newCollection()
	{
		return new TCNameList();
	}

	@Override
	public TCNameList caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}

	@Override
	public TCNameList caseVariableExpression(INVariableExpression node, Object arg)
	{
		if (node.name.isOld())
		{
			return new TCNameList(node.name);
		}
		else
		{
			return new TCNameList();
		}
	}
}
