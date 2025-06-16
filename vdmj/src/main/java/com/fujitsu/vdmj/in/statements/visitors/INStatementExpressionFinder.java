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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.statements.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INStatement;

/**
 * Find an expression by line number within a statement.
 */
public class INStatementExpressionFinder extends INLeafStatementVisitor<INExpression, INExpressionList, Integer>
{
	public INStatementExpressionFinder(INVisitorSet<INExpression, INExpressionList, Integer> visitors)
	{
		super(true);	// So we visit the nodes as well as the leaves
		visitorSet = visitors;
	}

	@Override
	protected INExpressionList newCollection()
	{
		return new INExpressionList();
	}

	@Override
	protected INExpressionList caseNonLeafNode(INStatement node, Integer arg)
	{
		return caseStatement(node, arg);
	}

	@Override
	public INExpressionList caseStatement(INStatement node, Integer lineno)
	{
		return newCollection();
	}
}
