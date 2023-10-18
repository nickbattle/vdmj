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

import com.fujitsu.vdmj.po.expressions.POCompExpression;

public class FuncComposeObligation extends ProofObligation
{
	public FuncComposeObligation(
		POCompExpression exp, String pref1, String pref2, POContextStack ctxt)
	{
		super(exp.location, POType.FUNC_COMPOSE, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("forall arg:");
		sb.append(explicitType(exp.rtype.getFunction().parameters.get(0), exp.location));
		sb.append(" & ");

		if (pref2 == null || !pref2.equals(""))
		{
    		if (pref2 != null)
    		{
        		sb.append(pref2);
        		sb.append("(arg) => ");
    		}
    		else
    		{
        		sb.append("pre_(");
        		sb.append(exp.right);
        		sb.append(", arg) => ");
    		}
		}

		if (pref1 != null)
		{
    		sb.append(pref1);
    		sb.append("(");
    		sb.append(exp.right);
    		sb.append("(arg))");
		}
		else
		{
    		sb.append("pre_(");
    		sb.append(exp.left);
    		sb.append(", ");
    		sb.append(exp.right);
    		sb.append("(arg))");
		}

		value = ctxt.getObligation(sb.toString());
	}
}
