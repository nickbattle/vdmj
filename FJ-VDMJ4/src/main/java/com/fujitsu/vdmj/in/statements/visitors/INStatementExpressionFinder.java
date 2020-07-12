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

package com.fujitsu.vdmj.in.statements.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.definitions.INAssignmentDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INValueDefinition;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionFinder;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.statements.INStatement;

/**
 * Find an expression by line number within a statement.
 */
public class INStatementExpressionFinder extends INLeafStatementVisitor<INExpression, INExpressionList, Integer>
{
	private class VisitorSet extends INVisitorSet<INExpression, INExpressionList, Integer>
	{
		private final INExpressionVisitor<INExpressionList, Integer> expVisitor = new INExpressionFinder();

		@Override
		public INExpressionVisitor<INExpressionList, Integer> getExpressionVisitor()
		{
			return expVisitor;
		}

		@Override
		public INDefinitionVisitor<INExpressionList, Integer> getDefinitionVisitor()
		{
			return new INDefinitionVisitor<INExpressionList, Integer>()
			{
				@Override
				public INExpressionList caseDefinition(INDefinition node, Integer lineno)
				{
					return new INExpressionList();
				}
				
				/**
				 * Assignment definitions are "dcl var:type := exp"
				 */
				@Override
				public INExpressionList caseAssignmentDefinition(INAssignmentDefinition node, Integer lineno)
				{
					INExpressionList list = newCollection();
					
					if (node.expression.location.startLine == lineno)
					{
						list.add(node.expression);		// Could apply exp visitor...
					}

					return list;
				}
				
				/**
				 * Value definitions are in let def statements.
				 */
				@Override
				public INExpressionList caseValueDefinition(INValueDefinition node, Integer lineno)
				{
					INExpressionList list = newCollection();
					
					if (node.exp.location.startLine == lineno)
					{
						list.add(node.exp);		// Could apply exp visitor...
					}

					return list;
				}
			};
		}
	}
	
	public INStatementExpressionFinder()
	{
		super(true);	// So we visit the nodes as well as the leaves
		visitorSet = new VisitorSet();
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
