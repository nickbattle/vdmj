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

/**
 * Search an expression tree after a QuickCheck execution to decide the outcome.
 * This is affected by the forall/exists subexpressions, whether they have
 * type bindings set and whether the binds have all type values. 
 */
public class MaybeFinder extends INLeafExpressionVisitor<Object, List<Object>, Object>
{
	private int maybeCount = 0; 
	
	public MaybeFinder()
	{
		super(false);
	}
	
	public boolean hasMaybe()
	{
		return maybeCount > 0;
	}

	@Override
	protected List<Object> newCollection()
	{
		return new Vector<Object>();
	}
	
	@Override
	public List<Object> caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
	
	@Override
	public List<Object> caseForAllExpression(INForAllExpression node, Object arg)
	{
		if (node.maybe)
		{
			maybeCount++;
			node.maybe = false;
		}
		
		return super.caseForAllExpression(node, arg);
	}
	
	@Override
	public List<Object> caseExistsExpression(INExistsExpression node, Object arg)
	{
		if (node.maybe)
		{
			maybeCount++;
			node.maybe = false;
		}
		
		return super.caseExistsExpression(node, arg);
	}
}
