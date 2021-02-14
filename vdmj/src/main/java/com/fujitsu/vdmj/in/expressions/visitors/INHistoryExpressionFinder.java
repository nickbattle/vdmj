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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.expressions.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INHistoryExpression;

public class INHistoryExpressionFinder extends INLeafExpressionVisitor<INExpression, INExpressionList, Object>
{
	public INHistoryExpressionFinder()
	{
		super(false);
		visitorSet = new INVisitorSet<INExpression, INExpressionList, Object>() {};
	}

	@Override
	protected INExpressionList newCollection()
	{
		return new INExpressionList();
	}

	@Override
	public INExpressionList caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}

	@Override
	public INExpressionList caseHistoryExpression(INHistoryExpression node, Object arg)
	{
		INExpressionList result = new INExpressionList();
		result.add(node);
		return result;
	}
}
