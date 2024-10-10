/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POExists1Expression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POLetBeStExpression;
import com.fujitsu.vdmj.po.expressions.POMapCompExpression;
import com.fujitsu.vdmj.po.expressions.POSeqCompExpression;
import com.fujitsu.vdmj.po.expressions.POSetCompExpression;

public class POForAllPredicateContext extends POForAllContext
{
	public final POExpression predicate;

	public POForAllPredicateContext(POMapCompExpression exp)
	{
		super(exp);
		this.predicate = exp.predicate;
	}

	public POForAllPredicateContext(POSetCompExpression exp)
	{
		super(exp);
		this.predicate = exp.predicate;
	}

	public POForAllPredicateContext(POSeqCompExpression exp)
	{
		super(exp);
		this.predicate = exp.predicate;
	}

	public POForAllPredicateContext(POExists1Expression exp)
	{
		super(exp);
		this.predicate = exp.predicate;
	}

	public POForAllPredicateContext(POLetBeStExpression exp)
	{
		super(exp);
		this.predicate = exp.suchThat;
	}

	@Override
	public String getSource()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(super.getSource());

		if (predicate != null)
		{
			sb.append(" ");
			sb.append(predicate);
			sb.append(" =>");
		}

		return sb.toString();
	}
}
