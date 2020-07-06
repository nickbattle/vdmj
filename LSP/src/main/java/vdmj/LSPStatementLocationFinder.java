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
import com.fujitsu.vdmj.tc.statements.TCAssignmentStatement;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCFieldDesignator;
import com.fujitsu.vdmj.tc.statements.TCIdentifierDesignator;
import com.fujitsu.vdmj.tc.statements.TCLeafStatementVisitor;
import com.fujitsu.vdmj.tc.statements.TCMapSeqDesignator;
import com.fujitsu.vdmj.tc.statements.TCStateDesignator;
import com.fujitsu.vdmj.tc.statements.TCStatement;

public class LSPStatementLocationFinder extends TCLeafStatementVisitor<TCNode, Set<TCNode>, LexLocation>
{
	public LSPStatementLocationFinder(TCVisitorSet<TCNode, Set<TCNode>, LexLocation> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}

	@Override
	public Set<TCNode> caseStatement(TCStatement node, LexLocation arg)
	{
		return newCollection();		// Default is "nothing found"
	}

 	@Override
	public Set<TCNode> caseCallObjectStatement(TCCallObjectStatement node, LexLocation arg)
	{
		Set<TCNode> all = newCollection();

		if (arg.within(node.location) ||
			(node.classname != null && arg.within(node.classname.getLocation())) ||
			(node.fieldname != null && arg.within(node.fieldname.getLocation())))
		{
			all.add(node);
		}

		all.addAll(super.caseCallObjectStatement(node, arg));
		return all;
	}

 	@Override
	public Set<TCNode> caseCallStatement(TCCallStatement node, LexLocation arg)
	{
		Set<TCNode> all = newCollection();

		if (arg.within(node.location))
		{
			all.add(node);
		}

		all.addAll(super.caseCallStatement(node, arg));
		return all;
	}
 	
 	@Override
 	public Set<TCNode> caseAssignmentStatement(TCAssignmentStatement node, LexLocation arg)
 	{
		Set<TCNode> all = newCollection();
		TCStateDesignator des = node.target;
		boolean found = false;
		
		while (true)
		{
			if (des instanceof TCIdentifierDesignator)
			{
				found = found || arg.within(des.location);
				if (found) all.add(des);
				break;
			}
			else if (des instanceof TCMapSeqDesignator)
			{
				TCMapSeqDesignator ms = (TCMapSeqDesignator)des;
				des = ms.mapseq;
			}
			else if (des instanceof TCFieldDesignator)
			{
				TCFieldDesignator f = (TCFieldDesignator)des;
				des = f.object;
				found = found || arg.within(f.field.getLocation());
			}
		}

 		all.addAll(super.caseAssignmentStatement(node, arg));
		return all;
 	}
}
