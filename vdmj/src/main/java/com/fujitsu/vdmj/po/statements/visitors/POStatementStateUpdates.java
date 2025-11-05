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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.po.statements.visitors;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.annotations.POAnnotatedStatement;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionStateUpdates;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionStateUpdates;
import com.fujitsu.vdmj.po.patterns.visitors.POBindStateUpdates;
import com.fujitsu.vdmj.po.patterns.visitors.POMultipleBindStateUpdates;
import com.fujitsu.vdmj.po.statements.POAssignmentStatement;
import com.fujitsu.vdmj.po.statements.POCallObjectStatement;
import com.fujitsu.vdmj.po.statements.POCallStatement;
import com.fujitsu.vdmj.po.statements.POExternalClause;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A visitor set to explore the PO tree and return the state names updated. This excludes any
 * names that are added within the scope.
 */
public class POStatementStateUpdates extends POLeafStatementVisitor<TCNameToken, TCNameSet, TCNameSet>
{
	private final POContextStack ctxt;
	
	public POStatementStateUpdates(POContextStack ctxt)
	{
		super(false);

		this.ctxt = ctxt;
		
		visitorSet = new POVisitorSet<TCNameToken, TCNameSet, TCNameSet>()
		{
			@Override
			protected void setVisitors()
			{
				statementVisitor = POStatementStateUpdates.this;
				definitionVisitor = new PODefinitionStateUpdates(this);
				expressionVisitor = new POExpressionStateUpdates(this);
				bindVisitor = new POBindStateUpdates(this);
				multiBindVisitor = new POMultipleBindStateUpdates(this);
			}

			@Override
			protected TCNameSet newCollection()
			{
				return POStatementStateUpdates.this.newCollection();
			}
		};
	}

	@Override
	public TCNameSet caseAnnotatedStatement(POAnnotatedStatement node, TCNameSet locals)
	{
		return node.statement.apply(this, locals);	// Don't process annotation's args
	}
	
	@Override
	public TCNameSet caseAssignmentStatement(POAssignmentStatement node, TCNameSet locals)
	{
		TCNameSet rhs = visitorSet.applyExpressionVisitor(node.exp, locals);
		TCNameToken target = node.target.updatedVariableName();

		if (!locals.contains(target))	// If it's not defined within the scope
		{
			rhs.add(target);
		}

		return rhs;
	}
	
	@Override
	public TCNameSet caseCallStatement(POCallStatement node, TCNameSet locals)
	{
		return operationCall(node.location, node.opdef);
	}
	
	@Override
	public TCNameSet caseCallObjectStatement(POCallObjectStatement node, TCNameSet locals)
	{
		return operationCall(node.location, node.fdef);
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(POStatement node, TCNameSet locals)
	{
		return newCollection();
	}

	/**
	 * Use the operation's pure and ext clauses to try to determine the variable
	 * access. This is also called from the expression visitor.
	 */
	public TCNameSet operationCall(LexLocation from, PODefinition def)
	{
		TCNameSet all = new TCNameSet();
		
		if (def == null)
		{
			all.addAll(ctxt.getStateVariables());	// Don't know!
		}
		else if (def.accessSpecifier.isPure)
		{
			// No updates by definition of pure
		}
		else if (!def.location.sameModule(from))
		{
			all.addAll(ctxt.getStateVariables());	// Remote call may call back!
		}
		else if (def instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition imp = (POImplicitOperationDefinition)def;
			
			if (imp.externals != null)
			{
				for (POExternalClause ext: imp.externals)
				{
					if (ext.mode.is(Token.WRITE))
					{
						all.addAll(ext.identifiers);
					}
				}
			}
			else
			{
				all.addAll(ctxt.getStateVariables());
			}
		}
		else if (def instanceof POExplicitOperationDefinition)
		{
			all.addAll(ctxt.getStateVariables());
		}

		return all;
	}
}
