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

package com.fujitsu.vdmj.po.statements.visitors;

import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionStateFinder;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionStateFinder;
import com.fujitsu.vdmj.po.statements.POAssignmentStatement;
import com.fujitsu.vdmj.po.statements.POCallObjectStatement;
import com.fujitsu.vdmj.po.statements.POCallStatement;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POLetDefStatement;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A visitor set to explore the PO tree and return the state names accessed.
 */
public class POStatementStateFinder extends POLeafStatementVisitor<TCNameToken, TCNameSet, Boolean>
{
	public POStatementStateFinder()
	{
		super(false);
		
		visitorSet = new POVisitorSet<TCNameToken, TCNameSet, Boolean>()
		{
			@Override
			protected void setVisitors()
			{
				definitionVisitor = new PODefinitionStateFinder(this);
				expressionVisitor = new POExpressionStateFinder(this);
				statementVisitor = POStatementStateFinder.this;
				patternVisitor = null;
				bindVisitor = null;
				multiBindVisitor = null;
			}

			@Override
			protected TCNameSet newCollection()
			{
				return POStatementStateFinder.this.newCollection();
			}
		};
	}
	
	@Override
	public TCNameSet caseAssignmentStatement(POAssignmentStatement node, Boolean updates)
	{
		TCNameSet all = newCollection();
		
		if (updates)
		{
			if (node.target instanceof POIdentifierDesignator)
			{
				POIdentifierDesignator id = (POIdentifierDesignator)node.target;
				all.add(id.name);
			}
			else
			{
				// Updates something...
				all.add(new TCNameToken(node.location, "?", node.target.toString()));
			}
		}
		
		return all;
	}
	
	@Override
	public TCNameSet caseCallStatement(POCallStatement node, Boolean updates)
	{
		TCNameSet all = newCollection();
		all.add(new TCNameToken(node.location, "?", "?"));	// Not state, but assumed to access state.
		return all;
	}
	
	@Override
	public TCNameSet caseCallObjectStatement(POCallObjectStatement node, Boolean updates)
	{
		TCNameSet all = newCollection();
		all.add(new TCNameToken(node.location, "?", "?"));	// Not state, but assumed to access state.
		return all;
	}
	
	@Override
	public TCNameSet caseLetDefStatement(POLetDefStatement node, Boolean updates)
	{
		TCNameSet all = newCollection();
		
		for (PODefinition def: node.localDefs)
		{
			if (def instanceof POValueDefinition)
			{
				POValueDefinition vdef = (POValueDefinition)def;
				
				if (vdef.exp.readsState())
				{
					for (PODefinition ldef: vdef.defs)
					{
						ldef.setNameScope(NameScope.STATE);		// eg. "let x = s1 + s2 in..." makes x STATE
					}
				}
			}
		}
		
		all.addAll(super.caseLetDefStatement(node, updates));
		return all;
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(POStatement node, Boolean updates)
	{
		return newCollection();
	}
}
