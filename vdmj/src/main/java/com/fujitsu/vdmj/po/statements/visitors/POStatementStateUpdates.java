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
import com.fujitsu.vdmj.po.statements.POFieldDesignator;
import com.fujitsu.vdmj.po.statements.POForAllStatement;
import com.fujitsu.vdmj.po.statements.POForIndexStatement;
import com.fujitsu.vdmj.po.statements.POForPatternBindStatement;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POMapSeqDesignator;
import com.fujitsu.vdmj.po.statements.POStateDesignator;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.po.statements.POWhileStatement;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A visitor set to explore the PO tree and return the state names updated.
 */
public class POStatementStateUpdates extends POLeafStatementVisitor<TCNameToken, TCNameSet, Object>
{
	private static TCNameToken EVERYTHING = new TCNameToken(LexLocation.ANY, "*", "*");
	private boolean firstLoop = false;
	
	public POStatementStateUpdates()
	{
		super(false);
		
		visitorSet = new POVisitorSet<TCNameToken, TCNameSet, Object>()
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
	public TCNameSet caseAnnotatedStatement(POAnnotatedStatement node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public TCNameSet caseAssignmentStatement(POAssignmentStatement node, Object arg)
	{
		return designatorUpdates(node.target);
	}
	
	@Override
	public TCNameSet caseWhileStatement(POWhileStatement node, Object arg)
	{
		if (firstLoop)
		{
			firstLoop = false;
			return super.caseWhileStatement(node, arg);
		}
		
		return newCollection();		// Don't nest loops
	}

	@Override
	public TCNameSet caseForAllStatement(POForAllStatement node, Object arg)
	{
		if (firstLoop)
		{
			firstLoop = false;
			return super.caseForAllStatement(node, arg);
		}
		
		return newCollection();		// Don't nest loops
	}

	@Override
	public TCNameSet caseForIndexStatement(POForIndexStatement node, Object arg)
	{
		if (firstLoop)
		{
			firstLoop = false;
			return super.caseForIndexStatement(node, arg);
		}
		
		return newCollection();		// Don't nest loops
	}

	@Override
	public TCNameSet caseForPatternBindStatement(POForPatternBindStatement node, Object arg)
	{
		if (firstLoop)
		{
			firstLoop = false;
			return super.caseForPatternBindStatement(node, arg);
		}
		
		return newCollection();		// Don't nest loops
	}

	@Override
	public TCNameSet caseCallStatement(POCallStatement node, Object arg)
	{
		return operationCall(node.opdef);
	}
	
	@Override
	public TCNameSet caseCallObjectStatement(POCallObjectStatement node, Object arg)
	{
		return operationCall(node.fdef);
	}
	
	@Override
	protected TCNameSet newCollection()
	{
		return new TCNameSet();
	}

	@Override
	public TCNameSet caseStatement(POStatement node, Object arg)
	{
		return newCollection();
	}

	/**
	 * Identify the state names that are updated by a given state designator.
	 */
	private TCNameSet designatorUpdates(POStateDesignator sd)
	{
		TCNameSet all = newCollection();
		
		if (sd instanceof POIdentifierDesignator)
		{
			POIdentifierDesignator id = (POIdentifierDesignator)sd;
			all.add(id.name);
		}
		else if (sd instanceof POFieldDesignator)
		{
			POFieldDesignator fd = (POFieldDesignator)sd;
			all.addAll(designatorUpdates(fd.object));
		}
		else if (sd instanceof POMapSeqDesignator)
		{
			POMapSeqDesignator msd = (POMapSeqDesignator)sd;
			all.addAll(designatorUpdates(msd.mapseq));
		}
		
		return all;
	}

	/**
	 * Use the operation's pure and ext clauses to try to determine the variable
	 * access.
	 */
	public static TCNameSet operationCall(PODefinition def)
	{
		TCNameSet all = new TCNameSet();
		
		if (def == null)
		{
			all.add(EVERYTHING);	// Don't know!
		}
		else if (def.accessSpecifier.isPure)
		{
			// No updates by definition of pure
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
				all.add(EVERYTHING);
			}
		}
		else if (def instanceof POExplicitOperationDefinition)
		{
			all.add(EVERYTHING);
		}

		return all;
	}
}
