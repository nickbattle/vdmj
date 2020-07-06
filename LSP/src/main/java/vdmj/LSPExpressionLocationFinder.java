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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCValueDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCFuncInstantiationExpression;
import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;

public class LSPExpressionLocationFinder extends TCLeafExpressionVisitor<TCNode, Set<TCNode>, LexLocation>
{
	public LSPExpressionLocationFinder(TCVisitorSet<TCNode, Set<TCNode>, LexLocation> visitors)
	{
		visitorSet = visitors;
	}

	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}

	@Override
	public Set<TCNode> caseExpression(TCExpression node, LexLocation arg)
	{
		return newCollection();
	}
	
	@Override
	public Set<TCNode> caseMkTypeExpression(TCMkTypeExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseMkTypeExpression(node, arg);
		
		if (arg.within(node.typename.getLocation()))
		{
			all.add(node);
		}
		
		return all;
	}

	@Override
	public Set<TCNode> caseVariableExpression(TCVariableExpression node, LexLocation arg)
	{
		Set<TCNode> result = newCollection();

		if (arg.within(node.location))
		{
			result.add(node);
		}

		return result;
	}
	
	@Override
	public Set<TCNode> caseLetDefExpression(TCLetDefExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseLetDefExpression(node, arg);

		for (TCDefinition def: node.localDefs)
 		{
 			if (def instanceof TCValueDefinition)
 			{
 				TCValueDefinition vdef = (TCValueDefinition)def;
 				
 				for (TCType type: vdef.unresolved)
 				{
 					TCUnresolvedType unresolved = (TCUnresolvedType)type;
 					
 					if (arg.within(unresolved.typename.getLocation()))
 					{
 						all.add(unresolved);
 					}
 				}
 			}
 		}
 		
		return all;
	}
	
	@Override
	public Set<TCNode> caseFuncInstantiationExpression(TCFuncInstantiationExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseFuncInstantiationExpression(node, arg);

		for (TCType type: node.unresolved)
		{
			TCUnresolvedType unresolved = (TCUnresolvedType)type;
			
			if (arg.within(unresolved.typename.getLocation()))
			{
				all.add(unresolved);
			}
		}
		
		return all;
	}
	
	@Override
	public Set<TCNode> caseFieldExpression(TCFieldExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseFieldExpression(node, arg);
		
		if (arg.within(node.field.getLocation()))
		{
			all.add(node);
		}
	
		return all;
	}
}
