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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.INVisitorSet;

public class INStatementFinder extends INLeafStatementVisitor<INStatement, INStatementList, Integer>
{
	public INStatementFinder()
	{
		super(true);	// So we visit the nodes as well as the leaves
		visitorSet = new INVisitorSet<INStatement, INStatementList, Integer>() {};
	}

	@Override
	protected INStatementList newCollection()
	{
		return new INStatementList();
	}

	@Override
	protected INStatementList caseNonLeafNode(INStatement node, Integer arg)
	{
		return caseStatement(node, arg);
	}

	@Override
	public INStatementList caseStatement(INStatement node, Integer lineno)
	{
		INStatementList list = newCollection();
		
		if (node.location.startLine == lineno)
		{
			list.add(node);
		}

		return list;
	}
}
