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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import com.fujitsu.vdmj.po.expressions.POMapInverseExpression;

public class InvariantObligation extends ProofObligation
{
	private InvariantObligation(POMapInverseExpression exp, POContextStack ctxt)
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

		source = ctxt.getSource(sb.toString());
		setObligationVars(ctxt, exp);
		setReasonsAbout(ctxt.getReasonsAbout());
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static ProofObligationList getAllPOs(POMapInverseExpression exp, POContextStack ctxt)
	{
		ProofObligationList results = new ProofObligationList();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new InvariantObligation(exp, choice));
		}
		
		return results;
	}
}
