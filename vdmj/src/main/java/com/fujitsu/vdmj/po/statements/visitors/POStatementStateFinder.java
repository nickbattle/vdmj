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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.POVisitorSet;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionStateFinder;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionStateFinder;
import com.fujitsu.vdmj.po.patterns.visitors.POBindStateFinder;
import com.fujitsu.vdmj.po.patterns.visitors.POMultipleBindStateFinder;
import com.fujitsu.vdmj.po.statements.POAssignmentStatement;
import com.fujitsu.vdmj.po.statements.POCallObjectStatement;
import com.fujitsu.vdmj.po.statements.POCallStatement;
import com.fujitsu.vdmj.po.statements.POExternalClause;
import com.fujitsu.vdmj.po.statements.POFieldDesignator;
import com.fujitsu.vdmj.po.statements.POIdentifierDesignator;
import com.fujitsu.vdmj.po.statements.POLetDefStatement;
import com.fujitsu.vdmj.po.statements.POMapSeqDesignator;
import com.fujitsu.vdmj.po.statements.POStateDesignator;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A visitor set to explore the PO tree and return the state names accessed.
 */
public class POStatementStateFinder extends POLeafStatementVisitor<TCNameToken, TCNameSet, Boolean>
{
	private static TCNameToken EVERYTHING = new TCNameToken(LexLocation.ANY, "*", "*");
	
	public POStatementStateFinder()
	{
		super(false);
		
		visitorSet = new POVisitorSet<TCNameToken, TCNameSet, Boolean>()
		{
			@Override
			protected void setVisitors()
			{
				statementVisitor = POStatementStateFinder.this;
				definitionVisitor = new PODefinitionStateFinder(this);
				expressionVisitor = new POExpressionStateFinder(this);
				bindVisitor = new POBindStateFinder(this);
				multiBindVisitor = new POMultipleBindStateFinder(this);
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
		if (updates)
		{
			return designatorUpdates(node.target);
		}
		else
		{
			return designatorReads(node.target);
		}
	}
	
	@Override
	public TCNameSet caseCallStatement(POCallStatement node, Boolean updates)
	{
		return operationCall(node.opdef, updates);
	}
	
	@Override
	public TCNameSet caseCallObjectStatement(POCallObjectStatement node, Boolean updates)
	{
		return operationCall(node.fdef, updates);
	}
	
	@Override
	public TCNameSet caseLetDefStatement(POLetDefStatement node, Boolean updates)
	{
		TCNameSet all = super.caseLetDefStatement(node, updates);
		
		for (PODefinition def: node.localDefs)
		{
			if (def instanceof POValueDefinition)
			{
				POValueDefinition vdef = (POValueDefinition)def;
				
				if (!vdef.exp.readsState().isEmpty())
				{
					for (PODefinition ldef: vdef.defs)
					{
						ldef.setNameScope(NameScope.STATE);		// eg. "let x = s1 + s2 in..." makes x STATE
					}
				}
			}
		}
		
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
	 * Identify the names that are read by a given state designator.
	 */
	private TCNameSet designatorReads(POStateDesignator sd)
	{
		if (sd instanceof POIdentifierDesignator)
		{
			return newCollection();
		}
		else if (sd instanceof POFieldDesignator)
		{
			POFieldDesignator fd = (POFieldDesignator)sd;
			return designatorReads(fd.object);
		}
		else if (sd instanceof POMapSeqDesignator)
		{
			POMapSeqDesignator msd = (POMapSeqDesignator)sd;
			TCNameSet all = designatorReads(msd.mapseq);
			all.addAll(visitorSet.applyExpressionVisitor(msd.exp, false));
			return all;
		}
		
		return newCollection();		
	}

	/**
	 * Use the operation's pure and ext clauses to try to determine the variable
	 * access.
	 */
	private TCNameSet operationCall(PODefinition def, boolean updates)
	{
		TCNameSet all = newCollection();
		
		if (def == null)
		{
			all.add(EVERYTHING);	// Don't know!
		}
		else if (def.accessSpecifier.isPure && updates)
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
					if (updates && ext.mode.is(Token.WRITE))
					{
						all.addAll(ext.identifiers);
					}
					else if (!updates && ext.mode.is(Token.READ))
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
