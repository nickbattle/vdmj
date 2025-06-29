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
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionStateFinder;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCAssignmentStatement;
import com.fujitsu.vdmj.tc.statements.TCBlockStatement;
import com.fujitsu.vdmj.tc.statements.TCFieldDesignator;
import com.fujitsu.vdmj.tc.statements.TCIdentifierDesignator;
import com.fujitsu.vdmj.tc.statements.TCMapSeqDesignator;
import com.fujitsu.vdmj.tc.statements.TCStateDesignator;
import com.fujitsu.vdmj.tc.statements.TCStatement;

/**
 * A visitor set to explore the TC tree and return the state names accessed.
 */
public class TCStatementStateFinder extends TCLeafStatementVisitor<TCNameToken, TCNameSet, Boolean>
{
	private boolean firstBlock = true;

	public TCStatementStateFinder()
	{
		super();
		
		visitorSet = new TCVisitorSet<TCNameToken, TCNameSet, Boolean>()
		{
			@Override
			protected void setVisitors()
			{
				statementVisitor = TCStatementStateFinder.this;
				expressionVisitor = new TCExpressionStateFinder(this);
			}

			@Override
			protected TCNameSet newCollection()
			{
				return TCStatementStateFinder.this.newCollection();
			}
		};
	}

	@Override
	public TCNameSet caseAnnotatedStatement(TCAnnotatedStatement node, Boolean nested)
	{
		if (nested)
		{
			return super.caseAnnotatedStatement(node, nested);
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	public TCNameSet caseAssignmentStatement(TCAssignmentStatement node, Boolean nested)
	{
		return designatorUpdates(node.target);
	}

	@Override
	public TCNameSet caseBlockStatement(TCBlockStatement node, Boolean nested)
	{
		if (nested || firstBlock)
		{
			firstBlock = false;
			return super.caseBlockStatement(node, nested);
		}
		else
		{
			return newCollection();
		}
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, Boolean nested)
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
