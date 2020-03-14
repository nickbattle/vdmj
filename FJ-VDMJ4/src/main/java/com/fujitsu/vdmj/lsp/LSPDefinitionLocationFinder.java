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

package com.fujitsu.vdmj.lsp;

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLeafDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.statements.TCLeafStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCLeafTypeVisitor;

public class LSPDefinitionLocationFinder extends TCLeafDefinitionVisitor<TCNode, Set<TCNode>, LexLocation>
{
	// Note, static to avoid constructor loops!
	private static LSPExpressionLocationFinder expVisitor = new LSPExpressionLocationFinder();
	private static LSPStatementLocationFinder stmtVisitor = new LSPStatementLocationFinder();
	private static LSPTypeLocationFinder typeVisitor = new LSPTypeLocationFinder();

	@Override
	public Set<TCNode> caseDefinition(TCDefinition node, LexLocation position)
	{
		return newCollection();		// Nothing found
	}

	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}
	
	@Override
	protected TCLeafExpressionVisitor<TCNode, Set<TCNode>, LexLocation> getExpressionVisitor()
	{
		return expVisitor;
	}
	
	@Override
	protected TCLeafStatementVisitor<TCNode, Set<TCNode>, LexLocation> getStatementVisitor()
	{
		return stmtVisitor;
	}

	@Override
	protected TCLeafTypeVisitor<TCNode, Set<TCNode>, LexLocation> getTypeVisitor()
	{
		return typeVisitor;
	}
}
