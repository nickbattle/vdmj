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

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INLeafExpressionVisitor;

/**
 * Find all the forall expressions in a PO.
 */
public class ForallExpressionFinder extends INLeafExpressionVisitor<INForAllExpression, List<INForAllExpression>, Object>
{
	public ForallExpressionFinder()
	{
		super(false);
	}

	@Override
	protected List<INForAllExpression> newCollection()
	{
		return new Vector<INForAllExpression>();
	}
	
	@Override
	public List<INForAllExpression> caseForAllExpression(INForAllExpression node, Object arg)
	{
		List<INForAllExpression> all = super.caseForAllExpression(node, arg);
		all.add(node);
		return all;
	}

	@Override
	public List<INForAllExpression> caseExpression(INExpression node, Object arg)
	{
		return newCollection();
	}
}
