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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.annotations.TCAnnotatedStatement;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionStateUpdates;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionStateUpdates;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.visitors.TCBindStateUpdates;
import com.fujitsu.vdmj.tc.patterns.visitors.TCMultipleBindStateUpdates;
import com.fujitsu.vdmj.tc.statements.TCAssignmentStatement;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCExternalClause;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.typechecker.Environment;

/**
 * A visitor set to explore the TC tree and return the state names updated.
 */
public class TCStatementStateUpdates extends TCLeafStatementVisitor<TCNameToken, TCNameSet, Environment>
{
	public TCStatementStateUpdates()
	{
		super();
		
		visitorSet = new TCVisitorSet<TCNameToken, TCNameSet, Environment>()
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
	public TCNameSet caseAnnotatedStatement(TCAnnotatedStatement node, Environment env)
	{
		return node.statement.apply(this, null);	// Don't process args
	}
	
	@Override
	public TCNameSet caseAssignmentStatement(TCAssignmentStatement node, Environment env)
	{
		return new TCNameSet(node.target.updatedVariableName());
	}

	@Override
	public TCNameSet caseCallStatement(TCCallStatement node, Environment env)
	{
		return operationCall(node.location, node.getDefinition(), env);
	}

	@Override
	public TCNameSet caseCallObjectStatement(TCCallObjectStatement node, Environment env)
	{
		return operationCall(node.location, node.getDefinition(), env);
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(TCStatement node, Environment env)
	{
		return newCollection();
	}

	private TCNameSet getStateVariables(Environment env)
	{
		TCStateDefinition state = env.findStateDefinition();
		TCNameSet all = newCollection();

		if (state != null)
		{
			for (TCField field: state.fields)
			{
				all.add(field.tagname);
			}
		}

		return all;
	}

	/**
	 * Use the operation's pure and ext clauses to try to determine the variable updates.
	 */
	private TCNameSet operationCall(LexLocation from, TCDefinition def, Environment env)
	{
		TCNameSet all = new TCNameSet();
		
		if (def == null)
		{
			all.addAll(getStateVariables(env));	// Don't know!
		}
		else if (def.accessSpecifier.isPure)
		{
			// No updates by definition of pure
		}
		else if (def instanceof TCImplicitOperationDefinition)
		{
			TCImplicitOperationDefinition imp = (TCImplicitOperationDefinition)def;
			
			if (imp.location.sameModule(from) && imp.externals != null)
			{
				for (TCExternalClause ext: imp.externals)
				{
					if (ext.mode.is(Token.WRITE))
					{
						all.addAll(ext.identifiers);
					}
				}
			}
			else
			{
				all.addAll(getStateVariables(env));
			}
		}
		else if (def instanceof TCExplicitOperationDefinition)
		{
			all.addAll(getStateVariables(env));
		}

		return all;
	}
}
