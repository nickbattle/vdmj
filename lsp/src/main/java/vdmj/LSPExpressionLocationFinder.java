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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCFuncInstantiationExpression;
import com.fujitsu.vdmj.tc.expressions.TCHistoryExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCNarrowExpression;
import com.fujitsu.vdmj.tc.expressions.TCNewExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

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
 			all.addAll(visitorSet.applyDefinitionVisitor(def, arg));
 		}
 		
		return all;
	}
	
	@Override
	public Set<TCNode> caseFuncInstantiationExpression(TCFuncInstantiationExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseFuncInstantiationExpression(node, arg);
		all.addAll(node.unresolved.matchUnresolved(arg));
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
	
	@Override
	public Set<TCNode> caseNewExpression(TCNewExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseNewExpression(node, arg);
		
		if (arg.within(node.classname.getLocation()))
		{
			if (node.ctordef != null)
			{
				all.add(node.ctordef.name);
			}
			else
			{
				all.add(node.classname);	// Anon ctor
			}
		}
	
		return all;
	}
	
	@Override
	public Set<TCNode> caseIsExpression(TCIsExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseIsExpression(node, arg);
		
		if (node.typename != null && arg.within(node.typename.getLocation()))
		{
			all.add(node.typename);
		}
		else if (node.unresolved != null)
		{
			all.addAll(node.unresolved.matchUnresolved(arg));
		}
	
		return all;
	}
	
	@Override
	public Set<TCNode> caseNarrowExpression(TCNarrowExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseNarrowExpression(node, arg);
		
		if (node.typename != null && arg.within(node.typename.getLocation()))
		{
			all.add(node.typename);
		}
		else if (node.unresolved != null)
		{
			all.addAll(node.unresolved.matchUnresolved(arg));
		}
	
		return all;
	}
	
	@Override
	public Set<TCNode> caseHistoryExpression(TCHistoryExpression node, LexLocation arg)
	{
		Set<TCNode> all = newCollection();
		
		for (TCNameToken opname: node.opnames)
		{
			if (arg.within(opname.getLocation()))
			{
				all.add(opname);
			}
		}
	
		return all;
	}
}
