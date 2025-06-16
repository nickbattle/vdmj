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

package com.fujitsu.vdmj.in.definitions.visitors;

import com.fujitsu.vdmj.in.INVisitorSet;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.INStatementList;
import com.fujitsu.vdmj.in.statements.visitors.INStatementFinder;

/**
 * Find an statement by line number within a definition.
 */
public class INDefinitionStatementFinder extends INLeafDefinitionVisitor<INStatement, INStatementList, Integer>
{
	public INDefinitionStatementFinder()
	{
		visitorSet = new INVisitorSet<INStatement, INStatementList, Integer>()
		{
			@Override
			protected void setVisitors()
			{
				statementVisitor = new INStatementFinder();
			}

			@Override
			protected INStatementList newCollection()
			{
				return INDefinitionStatementFinder.this.newCollection();
			}
		};
	}

	public INDefinitionStatementFinder(INVisitorSet<INStatement, INStatementList, Integer> visitors)
	{
		this.visitorSet = visitors;
	}

	@Override
	protected INStatementList newCollection()
	{
		return new INStatementList();
	}

	@Override
	public INStatementList caseDefinition(INDefinition node, Integer lineno)
	{
		return newCollection();
	}
}
