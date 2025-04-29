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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.expressions.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A visitor set to explore the TC tree and return the state names accessed.
 */
public class TCExpressionStateFinder extends TCLeafExpressionVisitor<TCNameToken, TCNameSet, Boolean>
{
	public TCExpressionStateFinder(TCVisitorSet<TCNameToken, TCNameSet, Boolean> visitors)
	{
		this.visitorSet = visitors;
	}
	
	@Override
	public TCNameSet caseVariableExpression(TCVariableExpression node, Boolean updates)
	{
		TCNameSet all = newCollection();
		
		if (!updates && node.vardef != null && node.vardef.nameScope.matches(NameScope.STATE))
		{
			all.add(node.name);
		}
		
		return all;
	}
	
	@Override
	public TCNameSet caseApplyExpression(TCApplyExpression node, Boolean updates)
	{
		TCNameSet all = super.caseApplyExpression(node, updates);
		
		if (node.type instanceof TCOperationType)
		{
			if (node.root instanceof TCVariableExpression)
			{
				TCVariableExpression name = (TCVariableExpression)node.root;
				all.add(name.name);
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
	public TCNameSet caseExpression(TCExpression node, Boolean updates)
	{
		return newCollection();
	}
}
