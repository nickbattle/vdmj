/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
package quickcheck.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.in.expressions.INExistsExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;
import com.fujitsu.vdmj.pog.POStatus;

/**
 * Search an expression tree after a QuickCheck execution to decide the outcome.
 * This is affected by the forall/exists subexpressions, whether they have
 * type bindings set and whether the binds have all type values. 
 */
public class ExecutionResultFinder extends INLeafExpressionVisitor<POStatus, List<POStatus>, Object>
{
	public ExecutionResultFinder(boolean allNodes)
	{
		super(allNodes);
	}

	@Override
	protected List<POStatus> newCollection()
	{
		return new Vector<POStatus>();
	}
	
	private List<POStatus> result(POStatus status)
	{
		List<POStatus> one = newCollection();
		one.add(status);
		return one;
	}

	@Override
	public List<POStatus> caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public List<POStatus> caseForAllExpression(INForAllExpression node, Object arg)
	{
		if (node.bindList.isInstrumented())
		{
			List<POStatus> inner = node.predicate.apply(this, arg);
			POStatus subexp = !inner.isEmpty() ? inner.get(0) : POStatus.UNPROVED;
			
			// if (!node.maybe)	// forall had everything
			{
				return result(POStatus.MAYBE);
			}
		}
		else
		{
			return super.caseForAllExpression(node, arg);
		}
	}
	
	@Override
	public List<POStatus> caseExistsExpression(INExistsExpression node, Object arg)
	{
		if (node.bindList.isInstrumented())
		{
			return result(POStatus.MAYBE);
		}
		else
		{
			return super.caseExistsExpression(node, arg);
		}
	}
}
