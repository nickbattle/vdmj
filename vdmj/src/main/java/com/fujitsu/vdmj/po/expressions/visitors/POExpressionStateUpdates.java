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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.expressions.visitors;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.visitors.POStatementStateUpdates;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A visitor set to explore the PO tree and return the state names updated.
 */
public class POExpressionStateUpdates extends POLeafExpressionVisitor<TCNameToken, TCNameSet, TCNameSet>
{
	public POExpressionStateUpdates(POVisitorSet<TCNameToken, TCNameSet, TCNameSet> visitors)
	{
		this.visitorSet = visitors;
	}
	
	@Override
	public TCNameSet caseApplyExpression(POApplyExpression node, TCNameSet locals)
	{
		TCNameSet all = super.caseApplyExpression(node, locals);

		if (node.opdef != null)		// Apply is an operation call
		{
			if (visitorSet.getStatementVisitor() instanceof POStatementStateUpdates)
			{
				// Call over to statement visitor's "operationCall" method
				POStatementStateUpdates stmt = (POStatementStateUpdates)visitorSet.getStatementVisitor();
				all.addAll(stmt.operationCall(node.location, node.opdef));
			}
		}

		return all;
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseExpression(POExpression node, TCNameSet locals)
	{
		return newCollection();
	}
}
