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
import com.fujitsu.vdmj.tc.definitions.TCLeafDefinitionVisitor;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.definitions.TCTypeDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.statements.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeVisitor;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;

public class LSPDefinitionLocationFinder extends TCLeafDefinitionVisitor<TCNode, Set<TCNode>, LexLocation>
{
	private LSPExpressionLocationFinder expVisitor = new LSPExpressionLocationFinder(this);
	private LSPStatementLocationFinder stmtVisitor = new LSPStatementLocationFinder(this);
	
	protected LSPDefinitionLocationFinder(TCVisitorSet<TCNode, Set<TCNode>, LexLocation> visitors)
	{
		super(visitors);
	}

	public LSPDefinitionLocationFinder()
	{
		super(null);
	}

	/**
	 * Search for types in the unresolved list that match the LexLocation sought. If there
	 * are any matches, there should only be one!
	 */
	private Set<TCNode> matchUnresolved(TCTypeList unresolvedList, LexLocation sought)
	{
		Set<TCNode> matched = newCollection();
		
		for (TCType type: unresolvedList)
		{
			TCUnresolvedType unresolved = (TCUnresolvedType)type;
			
			if (sought.within(unresolved.typename.getLocation()))
			{
				matched.add(unresolved);
			}
		}

		return matched;
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
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseTypeDefinition(TCTypeDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseTypeDefinition(node, sought);
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseLocalDefinition(TCLocalDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseLocalDefinition(node, sought);
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseValueDefinition(TCValueDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseValueDefinition(node, sought);
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseExplicitFunctionDefinition(TCExplicitFunctionDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseExplicitFunctionDefinition(node, sought);
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseImplicitFunctionDefinition(TCImplicitFunctionDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseImplicitFunctionDefinition(node, sought);
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}
	
	@Override
	public Set<TCNode> caseExplicitOperationDefinition(TCExplicitOperationDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseExplicitOperationDefinition(node, sought);
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}

	@Override
	public Set<TCNode> caseImplicitOperationDefinition(TCImplicitOperationDefinition node, LexLocation sought)
	{
		Set<TCNode> all = super.caseImplicitOperationDefinition(node, sought);
		all.addAll(matchUnresolved(node.unresolved, sought));
		return all;
	}
	
	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
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
	public TCTypeVisitor<Set<TCNode>, LexLocation> getTypeVisitor()
	{
		return null;
	}
}
