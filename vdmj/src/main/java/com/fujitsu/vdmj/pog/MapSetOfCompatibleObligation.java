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

import com.fujitsu.vdmj.po.expressions.POExpression;

public class MapSetOfCompatibleObligation extends ProofObligation
{
	private MapSetOfCompatibleObligation(POExpression exp, POContextStack ctxt)
	{
		super(exp.location, POType.MAP_SET_OF_COMPATIBLE, ctxt);
		StringBuilder sb = new StringBuilder();
		append(sb, exp.toString());
		source = ctxt.getSource(sb.toString());
		setObligationVars(ctxt, exp);
		setReasonsAbout(ctxt.getReasonsAbout());
	}

	private void append(StringBuilder sb, String exp)
	{
		String m1 = getVar("m");
		String m2 = getVar("m");

		sb.append("forall " + m1 + " in set ");
		sb.append(exp);
		sb.append(", " + m2 + " in set ");
		sb.append(exp);

		String d1 = getVar("d");
		String d2 = getVar("d");

		sb.append(" &\n  forall " + d1 + " in set dom " + m1 + ", " +
									d2 + " in set dom " + m2 + " &\n");
		sb.append("    " + d1 + " = " + d2 + " => " +
						m1 + "(" + d1 + ") = " + m2 + "(" + d2 + ")");
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static ProofObligationList getAllPOs(POExpression exp, POContextStack ctxt)
	{
		ProofObligationList results = new ProofObligationList();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new MapSetOfCompatibleObligation(exp, choice));
		}
		
		return results;
	}
}
