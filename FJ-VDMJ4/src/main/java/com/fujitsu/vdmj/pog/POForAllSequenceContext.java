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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POBind;

public class POForAllSequenceContext extends POContext
{
	public final POBind bind;
	public final POExpression sequence;

	public POForAllSequenceContext(POBind bind, POExpression exp)
	{
		this.bind = bind;
		this.sequence = exp;
	}

	@Override
	public String getContext()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("forall ");
		sb.append(bind.pattern);
		sb.append(" in set elems ");
		sb.append(sequence);
		sb.append(" & ");

		return sb.toString();
	}
}
