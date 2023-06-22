/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.qcplugins;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INGreaterExpression;
import com.fujitsu.vdmj.in.expressions.INIntegerLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INVariableExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;

public class SearchQCVisitor extends INLeafExpressionVisitor<NameValuePair, NameValuePairList, Object>
{
	public SearchQCVisitor()
	{
		super(false);
	}

	@Override
	public NameValuePairList caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public NameValuePairList caseGreaterExpression(INGreaterExpression node, Object arg)
	{
		NameValuePairList nvpl = newCollection();
		
		if (node.left instanceof INVariableExpression &&
			node.right instanceof INIntegerLiteralExpression)
		{
			INVariableExpression var = (INVariableExpression)node.left;
			INIntegerLiteralExpression rhs = (INIntegerLiteralExpression)node.right;
			
			nvpl.add(var.name, new IntegerValue(rhs.value.value - 1));	// ie. NOT > rhs
		}
		
		return nvpl;
	}

	@Override
	protected NameValuePairList newCollection()
	{
		return new NameValuePairList();
	}
}
