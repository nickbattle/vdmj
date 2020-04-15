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
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;

public class LSPExpressionLocationFinder extends TCLeafExpressionVisitor<TCNode, Set<TCNode>, LexLocation>
{
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
}
