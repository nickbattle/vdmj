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
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/
package com.fujitsu.vdmj.tc.expressions.visitors;

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCExpressionOpCallFinder extends TCLeafExpressionVisitor<TCNameToken, TCNameSet, Environment>
{
	public TCExpressionOpCallFinder()
	{
		// default visitorSet
	}

	@Override
	public TCNameSet caseExpression(TCExpression node, Environment env)
	{
		return newCollection();
	}
	
	@Override
	public TCNameSet caseApplyExpression(TCApplyExpression node, Environment env)
	{
		TCNameSet result = newCollection();
		TCDefinition opdef = null;

		if (node.root instanceof TCVariableExpression)
		{
			TCVariableExpression exp = (TCVariableExpression)node.root;
			opdef = env.findName(exp.name, NameScope.NAMESANDSTATE);
		}
		else if (node.root instanceof TCFieldExpression)
		{
			TCFieldExpression exp = (TCFieldExpression)node.root;
			opdef = env.findName(exp.memberName, NameScope.NAMESANDSTATE);
		}

		if (opdef != null && opdef.isOperation())
		{
			result.add(opdef.name);
		}

		result.addAll(super.caseApplyExpression(node, env));
		return result;
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}
}
