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

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POMapInverseExpression;
import com.fujitsu.vdmj.tc.types.TCInvariantType;

public class InvariantObligation extends ProofObligation
{
	public InvariantObligation(POExpression arg, TCInvariantType inv, POContextStack ctxt)
	{
		super(arg.location, POType.INVARIANT, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append(inv.invdef.name.getName());
		sb.append("(");
		sb.append(arg);
		sb.append(")");

		value = ctxt.getObligation(sb.toString());
	}

	public InvariantObligation(POMapInverseExpression exp, POContextStack ctxt)
	{
		super(exp.location, POType.INVARIANT, ctxt);
		StringBuilder sb = new StringBuilder();

		sb.append("is_(");
		sb.append(exp.exp);
		sb.append(", inmap ");
		sb.append(explicitType(exp.type.from, exp.location));
		sb.append(" to ");
		sb.append(explicitType(exp.type.to, exp.location));
		sb.append(")");

		value = ctxt.getObligation(sb.toString());
	}
}
