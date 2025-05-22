/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

package com.fujitsu.vdmj.po.expressions.visitors;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.visitors.POBindVariableFinder;
import com.fujitsu.vdmj.po.patterns.visitors.POMultipleBindVariableFinder;
import com.fujitsu.vdmj.po.patterns.visitors.POPatternVariableFinder;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A visitor set to explore the PO tree and return the variable names accessed.
 */
public class POExpressionVariableFinder extends POLeafExpressionVisitor<TCNameToken, TCNameSet, Object>
{
	public static TCNameToken SOMETHING = new TCNameToken(LexLocation.ANY, "?", "?");
	
	public POExpressionVariableFinder()
	{
		visitorSet = new POVisitorSet<TCNameToken, TCNameSet, Object>()
		{
			@Override
			protected void setVisitors()
			{
				expressionVisitor = POExpressionVariableFinder.this;
				patternVisitor = new POPatternVariableFinder(this);
				bindVisitor = new POBindVariableFinder(this);
				multiBindVisitor = new POMultipleBindVariableFinder(this);
			}

			@Override
			protected TCNameSet newCollection()
			{
				return POExpressionVariableFinder.this.newCollection();
			}
		};
	}
	
	@Override
	public TCNameSet caseApplyExpression(POApplyExpression node, Object arg)
	{
		TCNameSet names = super.caseApplyExpression(node, arg);
		
		if (node.opdef instanceof POExplicitOperationDefinition ||
			node.opdef instanceof POImplicitOperationDefinition)
		{
			names.add(SOMETHING);
		}
		
		return names;
	}
	
	@Override
	public TCNameSet caseVariableExpression(POVariableExpression node, Object arg)
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
	public TCNameSet caseExpression(POExpression node, Object arg)
	{
		return newCollection();
	}
}
