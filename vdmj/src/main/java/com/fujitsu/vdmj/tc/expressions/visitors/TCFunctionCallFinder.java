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
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class TCFunctionCallFinder extends TCLeafExpressionVisitor<TCNameToken, TCNameList, Object>
{
	public TCFunctionCallFinder()
	{
		// default visitorSet
	}

	@Override
	public TCNameList caseExpression(TCExpression node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public TCNameList caseApplyExpression(TCApplyExpression node, Object arg)
	{
		TCNameList result = newCollection();

		if (node.root instanceof TCVariableExpression)
		{
			TCVariableExpression vexp = (TCVariableExpression)node.root;
			result.add(vexp.name);
		}
		else if (node.root instanceof TCFuncInstantiationExpression)
		{
			TCFuncInstantiationExpression fie = (TCFuncInstantiationExpression)node.root;

			if (fie.function instanceof TCVariableExpression)
			{
				TCVariableExpression vexp = (TCVariableExpression)fie.function;
				result.add(vexp.name);
			}
		}

		result.addAll(super.caseApplyExpression(node, arg));
		return result;
	}

	@Override
	protected TCNameList newCollection()
	{
		return new TCNameList();
	}
}
