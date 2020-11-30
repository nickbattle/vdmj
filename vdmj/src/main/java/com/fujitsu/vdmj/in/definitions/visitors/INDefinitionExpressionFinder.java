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

package com.fujitsu.vdmj.in.definitions.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionFinder;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.statements.visitors.INStatementExpressionFinder;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;

/**
 * Find an expression by line number within a definition.
 */
public class INDefinitionExpressionFinder extends INLeafDefinitionVisitor<INExpression, INExpressionList, Integer>
{
	private class VisitorSet extends INVisitorSet<INExpression, INExpressionList, Integer>
	{
		private final INDefinitionVisitor<INExpressionList, Integer> defVisitor;
		private final INStatementVisitor<INExpressionList, Integer> stmtVisitor = new INStatementExpressionFinder(this);
		private final INExpressionVisitor<INExpressionList, Integer> expVisitor = new INExpressionFinder(this);

		public VisitorSet(INDefinitionExpressionFinder parent)
		{
			defVisitor = parent;
		}
		
		@Override
		public INDefinitionVisitor<INExpressionList, Integer> getDefinitionVisitor()
		{
			return defVisitor;
		}

		@Override
		public INStatementVisitor<INExpressionList, Integer> getStatementVisitor()
		{
			return stmtVisitor;
		}
		
		@Override
		public INExpressionVisitor<INExpressionList, Integer> getExpressionVisitor()
		{
			return expVisitor;
		}
	}
	
	public INDefinitionExpressionFinder()
	{
		super();
		visitorSet = new VisitorSet(this);
	}

	@Override
	protected INExpressionList newCollection()
	{
		return new INExpressionList();
	}

	@Override
	public INExpressionList caseDefinition(INDefinition node, Integer lineno)
	{
		return newCollection();
	}
}
