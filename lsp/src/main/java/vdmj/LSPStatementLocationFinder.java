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
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.statements.TCAssignmentStatement;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCFieldDesignator;
import com.fujitsu.vdmj.tc.statements.TCIdentifierDesignator;
import com.fujitsu.vdmj.tc.statements.TCMapSeqDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectApplyDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectFieldDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectIdentifierDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectNewDesignator;
import com.fujitsu.vdmj.tc.statements.TCObjectSelfDesignator;
import com.fujitsu.vdmj.tc.statements.TCStateDesignator;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.visitors.TCLeafStatementVisitor;

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
		Set<TCNode> all = caseObjectDesignator(node.designator, arg);
		
		if (node.classname != null && arg.within(node.classname.getLocation()))
		{
			all.add(node.classname);
		}
		
		if (node.field != null && arg.within(node.field.getLocation()))
		{
			all.add(node.field);
		}
		
		all.addAll(super.caseCallObjectStatement(node, arg));
		return all;
	}

 	private Set<TCNode> caseObjectDesignator(TCObjectDesignator node, LexLocation arg)
	{
 		Set<TCNode> all = newCollection();
 		
 		if (node instanceof TCObjectApplyDesignator)
 		{
 			TCObjectApplyDesignator apply = (TCObjectApplyDesignator)node;
 			TCExpressionVisitor<Set<TCNode>, LexLocation> expv = visitorSet.getExpressionVisitor();
 			
 			if (expv != null)
 			{
 				for (TCExpression a: apply.args)
 				{
 					all.addAll(a.apply(expv, arg));
 				}
 			}
 			
 			all.addAll(caseObjectDesignator(apply.object, arg));
 		}
 		else if (node instanceof TCObjectFieldDesignator)
 	 	{
 			TCObjectFieldDesignator fdes = (TCObjectFieldDesignator)node;
 			
			if (fdes.field != null && arg.within(fdes.field.getLocation()))
			{
				all.add(fdes.field);
			}
 			
 			all.addAll(caseObjectDesignator(fdes.object, arg));
 	 	}
 		else if (node instanceof TCObjectIdentifierDesignator)
 	 	{
 			TCObjectIdentifierDesignator id = (TCObjectIdentifierDesignator)node;
 			
 			if (arg.within(id.name.getLocation()))
 			{
 				all.add(id.expression);		// VariableExpression includes definition
 			}
 	 	}
 		else if (node instanceof TCObjectNewDesignator)
 	 	{
 			TCObjectNewDesignator des = (TCObjectNewDesignator)node;
 			TCExpressionVisitor<Set<TCNode>, LexLocation> expv = visitorSet.getExpressionVisitor();
 			
 			if (expv != null)
 			{
				all.addAll(des.expression.apply(expv, arg));
				
				if (arg.within(des.expression.classname.getLocation()))
				{
					if (des.expression.ctordef != null)
					{
						all.add(des.expression.ctordef.name);
					}
					else
					{
						all.add(des.expression.classname);	// Anon ctor
					}
				}
 			}
 	 	}
 		else if (node instanceof TCObjectSelfDesignator)
 	 	{
 			// ignore!
 	 	}
 	 	
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
