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

package com.fujitsu.vdmj.tc.statements.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionOpCallFinder;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCStatementOpCallFinder extends TCLeafStatementVisitor<TCNameToken, TCNameSet, Environment>
{
	public TCStatementOpCallFinder()
	{
		this.visitorSet = new TCVisitorSet<TCNameToken,TCNameSet,Environment>()
		{
			@Override
			protected void setVisitors()
			{
				statementVisitor = TCStatementOpCallFinder.this;
				expressionVisitor = new TCExpressionOpCallFinder();
			}

			@Override
			protected TCNameSet newCollection()
			{
				return new TCNameSet();
			}
		};
	}

	@Override
	public TCNameSet caseCallStatement(TCCallStatement node, Environment arg)
	{
		return new TCNameSet(node.name);
	}

	@Override
	public TCNameSet caseCallObjectStatement(TCCallObjectStatement node, Environment arg)
	{
		if (node.fdef != null)
		{
			return new TCNameSet(node.fdef.name);
		}
		
		return newCollection();
	}

	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, Environment arg)
	{
		return newCollection();
	}
}
