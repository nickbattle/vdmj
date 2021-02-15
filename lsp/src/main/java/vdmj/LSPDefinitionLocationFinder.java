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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCMutexSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCPerSyncDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.definitions.visitors.TCLeafDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.visitors.TCPatternVisitor;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;

public class LSPDefinitionLocationFinder extends TCLeafDefinitionVisitor<TCNode, Set<TCNode>, LexLocation>
{
	private static class VisitorSet extends TCVisitorSet<TCNode, Set<TCNode>, LexLocation>
	{
		private final LSPDefinitionLocationFinder defVisitor;
		private final LSPExpressionLocationFinder expVisitor;
		private final LSPStatementLocationFinder stmtVisitor;
		private final LSPPatternLocationFinder patVisitor;

		public VisitorSet(LSPDefinitionLocationFinder parent)
		{
			defVisitor = parent;
			expVisitor = new LSPExpressionLocationFinder(this);
			stmtVisitor = new LSPStatementLocationFinder(this);
			patVisitor = new LSPPatternLocationFinder();
		}
		
		@Override
		public TCDefinitionVisitor<Set<TCNode>, LexLocation> getDefinitionVisitor()
		{
			return defVisitor;
		}

		@Override
		public TCExpressionVisitor<Set<TCNode>, LexLocation> getExpressionVisitor()
	 	{
	 		return expVisitor;
	 	}
	 	
		@Override
		public TCStatementVisitor<Set<TCNode>, LexLocation> getStatementVisitor()
	 	{
	 		return stmtVisitor;
	 	}
		
		@Override
		public TCPatternVisitor<Set<TCNode>, LexLocation> getPatternVisitor()
		{
			return patVisitor;
		}
	}

	public LSPDefinitionLocationFinder()
	{
		visitorSet = new VisitorSet(this);
	}

	@Override
	public Set<TCNode> caseDefinition(TCDefinition node, LexLocation position)
	{
		return newCollection();		// Nothing found
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
	
	@Override
	protected Set<TCNode> caseMultipleBind(TCMultipleBind bind, LexLocation arg)
	{
		Set<TCNode> all = super.caseMultipleBind(bind, arg);
		
		if (all.isEmpty())
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mbind = (TCMultipleTypeBind)bind;
				all.addAll(mbind.unresolved.matchUnresolved(arg));
			}
		}
		
		return all;
	}
}
