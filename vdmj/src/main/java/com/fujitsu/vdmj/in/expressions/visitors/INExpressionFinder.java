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
import com.fujitsu.vdmj.in.expressions.INBinaryExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;

/**
 * Find an expression by line number within a root expression.
 */
public class INExpressionFinder extends INLeafExpressionVisitor<INExpression, INExpressionList, Integer>
{
	public INExpressionFinder()
	{
		super(true);	// So we visit the nodes as well as the leaves, default visitorSet
	}

	public INExpressionFinder(INVisitorSet<INExpression, INExpressionList, Integer> visitors)
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
	protected INExpressionList caseNonLeafNode(INExpression node, Integer arg)
	{
		return caseExpression(node, arg);
	}

	@Override
	public INExpressionList caseExpression(INExpression node, Integer lineno)
	{
		INExpressionList list = newCollection();
		
		if (node.location.startLine == lineno)
		{
			list.add(node);
		}

		return list;
	}

 	@Override
	public INExpressionList caseBinaryExpression(INBinaryExpression node, Integer lineno)
	{
 		// Note, we override to avoid caseNonLeafNode call, because binary expressions
 		// skip the breakpoint check, and so shouldn't be selected in the find.
 		INExpressionList all = newCollection();
		all.addAll(node.left.apply(this, lineno));
		all.addAll(node.right.apply(this, lineno));
		return all;
	}
}
