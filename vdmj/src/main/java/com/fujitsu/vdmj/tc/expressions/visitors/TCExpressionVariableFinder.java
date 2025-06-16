/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURTCSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions.visitors;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindVariableFinder;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindVariableFinder;
import com.fujitsu.vdmj.tc.patterns.visitors.TCPatternVariableFinder;

/**
 * A visitor set to explore the TC tree and return the variable names accessed.
 */
public class TCExpressionVariableFinder extends TCLeafExpressionVisitor<TCNameToken, TCNameSet, Object>
{
	public static TCNameToken SOMETHING = new TCNameToken(LexLocation.ANY, "?", "?");
	
	public TCExpressionVariableFinder()
	{
		visitorSet = new TCVisitorSet<TCNameToken, TCNameSet, Object>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = TCExpressionVariableFinder.this;
				patternVisitor = new TCPatternVariableFinder(this);
				bindVisitor = new TCBindVariableFinder(this);
				multiBindVisitor = new TCMultipleBindVariableFinder(this);
			}

			@Override
			protected TCNameSet newCollection()
			{
				return TCExpressionVariableFinder.this.newCollection();
			}
		};
	}
	
	@Override
	public TCNameSet caseVariableExpression(TCVariableExpression node, Object arg)
	{
		TCNameSet all = newCollection();
		all.add(node.name);
		return all;
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseExpression(TCExpression node, Object arg)
	{
		return newCollection();
	}
}
