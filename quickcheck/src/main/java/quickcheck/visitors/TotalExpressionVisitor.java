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

import com.fujitsu.vdmj.po.expressions.PODivideExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POLeafExpressionVisitor;

/**
 * Search a PO expression and note whether the expression is total (ie. defined for
 * all possible argument values).
 */
public class TotalExpressionVisitor extends POLeafExpressionVisitor<Boolean, List<Boolean>, Object>
{
	private boolean isTotal = true;		// Set false by some cases
	
	public boolean isTotal()
	{
		return isTotal;
	}
	
	@Override
	protected List<Boolean> newCollection()
	{
		return new Vector<Boolean>();
	}
	
	@Override
	public List<Boolean> caseDivideExpression(PODivideExpression node, Object arg)
	{
		isTotal = false;
		return newCollection();
	}

	@Override
	public List<Boolean> caseExpression(POExpression node, Object arg)
	{
		return newCollection();
	}
}
