/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package smtlib.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.tc.expressions.TCApplyExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCLeafExpressionVisitor;

public class ApplyFinder extends TCLeafExpressionVisitor<TCApplyExpression, List<TCApplyExpression>, Object>
{
	@Override
	protected List<TCApplyExpression> newCollection()
	{
		return new Vector<TCApplyExpression>();
	}

	@Override
	public List<TCApplyExpression> caseExpression(TCExpression node, Object arg)
	{
		return newCollection();
	}

	@Override
	public List<TCApplyExpression> caseApplyExpression(TCApplyExpression node, Object arg)
	{
		List<TCApplyExpression> list = super.caseApplyExpression(node, arg);
		list.add(node);
		return list;
	}
}
