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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.statements.visitors;

import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.annotations.TCAnnotatedStatement;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionStateUpdates;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionStateUpdates;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindStateUpdates;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindStateUpdates;
import com.fujitsu.vdmj.tc.statements.TCAssignmentStatement;
import com.fujitsu.vdmj.tc.statements.TCFieldDesignator;
import com.fujitsu.vdmj.tc.statements.TCIdentifierDesignator;
import com.fujitsu.vdmj.tc.statements.TCMapSeqDesignator;
import com.fujitsu.vdmj.tc.statements.TCStateDesignator;
import com.fujitsu.vdmj.tc.statements.TCStatement;

/**
 * A visitor set to explore the TC tree and return the state names accessed.
 */
public class TCStatementStateUpdates extends TCLeafStatementVisitor<TCNameToken, TCNameSet, Object>
{
	public TCStatementStateUpdates()
	{
		super();
		
		visitorSet = new TCVisitorSet<TCNameToken, TCNameSet, Object>()
		{
			@Override
			protected void setVisitors()
			{
				statementVisitor = TCStatementStateUpdates.this;
				definitionVisitor = new TCDefinitionStateUpdates(this);
				expressionVisitor = new TCExpressionStateUpdates(this);
				bindVisitor = new TCBindStateUpdates(this);
				multiBindVisitor = new TCMultipleBindStateUpdates(this);
			}

			@Override
			protected TCNameSet newCollection()
			{
				return TCStatementStateUpdates.this.newCollection();
			}
		};
	}

	@Override
	public TCNameSet caseAnnotatedStatement(TCAnnotatedStatement node, Object arg)
	{
		return node.statement.apply(this, null);	// Don't process args
	}
	
	@Override
	public TCNameSet caseAssignmentStatement(TCAssignmentStatement node, Object arg)
	{
		return designatorUpdates(node.target);
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, Object arg)
	{
		return newCollection();
	}

	/**
	 * Identify the state names that are updated by a given state designator.
	 */
	private TCNameSet designatorUpdates(TCStateDesignator sd)
	{
		TCNameSet all = newCollection();
		
		if (sd instanceof TCIdentifierDesignator)
		{
			TCIdentifierDesignator id = (TCIdentifierDesignator)sd;
			all.add(id.name);
		}
		else if (sd instanceof TCFieldDesignator)
		{
			TCFieldDesignator fd = (TCFieldDesignator)sd;
			all.addAll(designatorUpdates(fd.object));
		}
		else if (sd instanceof TCMapSeqDesignator)
		{
			TCMapSeqDesignator msd = (TCMapSeqDesignator)sd;
			all.addAll(designatorUpdates(msd.mapseq));
		}
		
		return all;
	}
}
