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

package com.fujitsu.vdmj.tc.expressions;

import java.util.TreeSet;

import com.fujitsu.vdmj.mapper.Mappable;

public class TCExpressionSet extends TreeSet<TCExpression> implements Mappable
{
	public TCExpressionSet()
	{
		super();
	}

	public TCExpressionSet(TCExpression... expressions)
	{
		for (TCExpression exp: expressions)
		{
			add(exp);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		for (TCExpression exp: this)
		{
			sb.append(exp.toString());
			sb.append("\n");
		}

		return sb.toString();
	}

	public TCExpressionList asList()
	{
		TCExpressionList list = new TCExpressionList();
		list.addAll(this);
		return list;
	}
}
