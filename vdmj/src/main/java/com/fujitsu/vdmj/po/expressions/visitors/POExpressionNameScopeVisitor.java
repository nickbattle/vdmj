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

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.expressions.POExpression;

/**
 * A visitor set to explore the PO tree and update the NameScopes accessed.
 */
public class POExpressionNameScopeVisitor extends POLeafExpressionVisitor<PONode, Set<PONode>, Object>
{
	public POExpressionNameScopeVisitor(POVisitorSet<PONode, Set<PONode>, Object> visitors)
	{
		this.visitorSet = visitors;
	}
	
	@Override
	protected Set<PONode> newCollection()
	{
		return new HashSet<PONode>();
	}

	@Override
	public Set<PONode> caseExpression(POExpression node, Object arg)
	{
		return newCollection();
	}

}
