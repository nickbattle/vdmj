/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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
package com.fujitsu.vdmj.tc.expressions.visitors;

import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFuncInstantiationExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TCApplyFinder extends TCLeafExpressionVisitor<TCExpression, TCExpressionList, TCNameToken>
{
	public TCApplyFinder()
	{
		// default visitorSet
	}

	@Override
	public TCExpressionList caseExpression(TCExpression node, TCNameToken arg)
	{
		return newCollection();
	}
	
	@Override
	public TCExpressionList caseApplyExpression(TCApplyExpression node, TCNameToken arg)
	{
		TCExpressionList result = newCollection();

		if (node.root instanceof TCVariableExpression)
		{
			TCVariableExpression vexp = (TCVariableExpression)node.root;

			if (vexp.name.equals(arg))
			{
				result.add(node);
			}
		}
		else if (node.root instanceof TCFuncInstantiationExpression)
		{
			TCFuncInstantiationExpression fie = (TCFuncInstantiationExpression)node.root;

			if (fie.function instanceof TCVariableExpression)
			{
				TCVariableExpression vexp = (TCVariableExpression)fie.function;

				if (vexp.name.equals(arg))
				{
					result.add(node);
				}
			}
		}

		result.addAll(super.caseApplyExpression(node, arg));
		return result;
	}

	@Override
	protected TCExpressionList newCollection()
	{
		return new TCExpressionList();
	}
}
