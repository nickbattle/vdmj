/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package vdmj;

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.TCVisitorSet;
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.definitions.visitors.TCLeafDefinitionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.TCExternalClause;

public class LSPDefinitionLocationFinder extends TCLeafDefinitionVisitor<TCNode, Set<TCNode>, LexLocation>
{
	public LSPDefinitionLocationFinder()
	{
		visitorSet = new TCVisitorSet<TCNode, Set<TCNode>, LexLocation>()
		{
			@Override
			protected void setVisitors()
			{
				definitionVisitor = LSPDefinitionLocationFinder.this;
				expressionVisitor = new LSPExpressionLocationFinder(this);
				statementVisitor = new LSPStatementLocationFinder(this);
				patternVisitor = new LSPPatternLocationFinder(this);
				bindVisitor = new LSPBindLocationFinder(this);
				multiBindVisitor = new LSPMultipleBindLocationFinder(this);
			}

			@Override
			protected Set<TCNode> newCollection()
			{
				return LSPDefinitionLocationFinder.this.newCollection();
			}
		};
	}

	@Override
	public Set<TCNode> caseDefinition(TCDefinition node, LexLocation position)
	{
		return newCollection();		// Nothing found
	}
	
	@Override
	public Set<TCNode> caseClassDefinition(TCClassDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseClassDefinition(node, sought);
		
		if (sought.within(node.name.getLocation()))
		{
			all.add(node.name);
		}
		else if (node.supernames != null)
		{
			for (TCNameToken sname: node.supernames)
			{
				if (sought.within(sname.getLocation()))
				{
					all.add(sname);
				}
			}
		}
		
		return all;
	}
	
	@Override
	public Set<TCNode> caseAssignmentDefinition(TCAssignmentDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseAssignmentDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseTypeDefinition(TCTypeDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseTypeDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseLocalDefinition(TCLocalDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseLocalDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseValueDefinition(TCValueDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseValueDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseExplicitFunctionDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseImplicitFunctionDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseExplicitOperationDefinition(TCExplicitOperationDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseExplicitOperationDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}

	@Override
	public Set<TCNode> caseImplicitOperationDefinition(TCImplicitOperationDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseImplicitOperationDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		
		if (node.externals != null)
		{
			for (TCExternalClause ext: node.externals)
			{
				for (TCNameToken name: ext.identifiers)
				{
					if (sought.within(name.getLocation()))
					{
						all.add(name);
					}
				}
				
				all.addAll(ext.unresolved.matchUnresolved(sought));
			}
		}
		
		return all;
	}
	
	@Override
	public Set<TCNode> caseStateDefinition(TCStateDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseStateDefinition(node, sought);
		all.addAll(node.unresolved.matchUnresolved(sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseMutexSyncDefinition(TCMutexSyncDefinition node, LexLocation sought)
	{
		Set<TCNode> all =  super.caseMutexSyncDefinition(node, sought);
		
		for (TCNameToken opname: node.operations)
		{
			if (sought.within(opname.getLocation()))
			{
				all.add(opname);
			}
		}
		
		return all;
	}
	
	@Override
	public Set<TCNode> casePerSyncDefinition(TCPerSyncDefinition node, LexLocation sought)
	{
		Set<TCNode> all =  super.casePerSyncDefinition(node, sought);
		
		if (sought.within(node.opname.getLocation()))
		{
			all.add(node.opname);
		}
		
		return all;
	}
	
	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}
}
