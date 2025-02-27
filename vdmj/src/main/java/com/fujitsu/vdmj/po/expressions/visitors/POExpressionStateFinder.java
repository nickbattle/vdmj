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

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A visitor set to explore the PO tree and return the state names accessed.
 */
public class POExpressionStateFinder extends POLeafExpressionVisitor<TCNameToken, TCNameSet, Boolean>
{
	public POExpressionStateFinder(POVisitorSet<TCNameToken, TCNameSet, Boolean> visitors)
	{
		this.visitorSet = visitors;
	}
	
	@Override
	public TCNameSet caseVariableExpression(POVariableExpression node, Boolean updates)
	{
		TCNameSet all = newCollection();
		
		if (!updates && node.vardef != null && node.vardef.nameScope.matches(NameScope.STATE))
		{
			all.add(node.name);
		}
		
		return all;
	}
	
	@Override
	public TCNameSet caseApplyExpression(POApplyExpression node, Boolean updates)
	{
		TCNameSet all = super.caseApplyExpression(node, updates);
		
		if (node.type instanceof TCOperationType)
		{
			if (node.root instanceof POVariableExpression)
			{
				POVariableExpression name = (POVariableExpression)node.root;
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
	public TCNameSet caseExpression(POExpression node, Boolean updates)
	{
		return newCollection();
	}
}
