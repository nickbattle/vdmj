/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;

public class OrderedObligation extends ProofObligation
{
	public OrderedObligation(POExpression left, POExpression right, TCTypeSet types, POContextStack ctxt)
	{
		super(left.location, POType.ORDERED, ctxt);
		StringBuilder sb = new StringBuilder();
		String prefix = "";

		for (TCType type: types)
		{
			sb.append(prefix);
    		sb.append("(is_(");
    		sb.append(left);
    		sb.append(", ");
    		sb.append(type);
    		sb.append(") and is_(");
    		sb.append(right);
    		sb.append(", ");
    		sb.append(type);
    		sb.append("))");
    		prefix = " or ";
		}
		
		value = ctxt.getObligation(sb.toString());
	}
}
