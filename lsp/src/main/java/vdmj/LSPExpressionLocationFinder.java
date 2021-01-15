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
import com.fujitsu.vdmj.tc.expressions.TCExists1Expression;
import com.fujitsu.vdmj.tc.expressions.TCExistsExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCForAllExpression;
import com.fujitsu.vdmj.tc.expressions.TCFuncInstantiationExpression;
import com.fujitsu.vdmj.tc.expressions.TCHistoryExpression;
import com.fujitsu.vdmj.tc.expressions.TCIsExpression;
import com.fujitsu.vdmj.tc.expressions.TCLetDefExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.patterns.TCTypeBind;

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
				all.addAll(vdef.unresolved.matchUnresolved(arg));
 			}
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
	public Set<TCNode> caseIsExpression(TCIsExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseIsExpression(node, arg);
		
		if (node.typename != null && arg.within(node.typename.getLocation()))
		{
			all.add(node);
		}
		// Can't find basic type as yet...!
	
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
	
	@Override
	public Set<TCNode> caseExistsExpression(TCExistsExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseExistsExpression(node, arg);
		
		for (TCMultipleBind bind: node.bindList)
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mbind = (TCMultipleTypeBind)bind;
				all.addAll(mbind.unresolved.matchUnresolved(arg));
			}
		}

		return all;
	}
	
	@Override
	public Set<TCNode> caseExists1Expression(TCExists1Expression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseExists1Expression(node, arg);
		
		if (node.bind instanceof TCTypeBind)
		{
			TCTypeBind tbind = (TCTypeBind)node.bind;
			all.addAll(tbind.unresolved.matchUnresolved(arg));
		}

		return all;
	}
	
	@Override
	public Set<TCNode> caseForAllExpression(TCForAllExpression node, LexLocation arg)
	{
		Set<TCNode> all = super.caseForAllExpression(node, arg);
		
		for (TCMultipleBind bind: node.bindList)
		{
			if (bind instanceof TCMultipleTypeBind)
			{
				TCMultipleTypeBind mbind = (TCMultipleTypeBind)bind;
				all.addAll(mbind.unresolved.matchUnresolved(arg));
			}
		}

		return all;
	}

	@Override
	protected Set<TCNode> caseBind(TCBind bind, LexLocation arg)
	{
		Set<TCNode> all = super.caseBind(bind, arg);
		
		if (all.isEmpty())
		{
			if (bind instanceof TCTypeBind)
			{
				TCTypeBind tbind = (TCTypeBind)bind;
				all.addAll(tbind.unresolved.matchUnresolved(arg));
			}
		}
		
		return all;
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
