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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * Look at a POExpression to find sub-expressions that would stop the POG from being
 * able to analyse further, such as operation calls.
 */
public class POStopsPOGVisitor extends POLeafExpressionVisitor<Boolean, List<Boolean>, Object>
{
	private boolean stopsPOG = false;		// Set true by some cases
	
	public boolean stopsPOG()
	{
		return stopsPOG;
	}

	@Override
	protected List<Boolean> newCollection()
	{
		return new Vector<Boolean>();
	}
	
	@Override
	public List<Boolean> caseApplyExpression(POApplyExpression node, Object arg)
	{
		if (node.type.isOperation(node.location))
		{
			stopsPOG = true;
		}
		
		return super.caseApplyExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseVariableExpression(POVariableExpression node, Object arg)
	{
		if (node.vardef != null && node.vardef.nameScope == NameScope.STATE)
		{
			stopsPOG = true;
		}
		
		return super.caseVariableExpression(node, arg);
	}

	@Override
	public List<Boolean> caseExpression(POExpression node, Object arg)
	{
		return newCollection();
	}
}
