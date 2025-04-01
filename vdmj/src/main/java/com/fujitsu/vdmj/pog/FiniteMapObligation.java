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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.expressions.POMapCompExpression;
import com.fujitsu.vdmj.po.patterns.POMultipleBind;
import com.fujitsu.vdmj.tc.types.TCType;

public class FiniteMapObligation extends ProofObligation
{
	private FiniteMapObligation(POMapCompExpression exp, TCType maptype, POContextStack ctxt)
	{
		super(exp.location, POType.FINITE_MAP, ctxt);
		StringBuilder sb = new StringBuilder();

		String finmap = getVar("finmap");
		String findex = getVar("findex");

		sb.append("exists " + finmap + ":map nat to (");
		sb.append(explicitType(maptype, exp.location));
		sb.append(") &\n  forall ");
		String prefix = "";

		for (POMultipleBind mb: exp.bindings)
		{
			sb.append(prefix);
			sb.append(mb);
			prefix = ", ";
		}

		sb.append(" &\n    ");
		
		if (exp.predicate != null)
		{
			sb.append(exp.predicate);
			sb.append(" => ");
		}
		
		sb.append("exists " + findex + " in set dom " + finmap + " & " +
			finmap + "(" + findex + ") = {");
		sb.append(exp.first);
		sb.append("}");

		source = ctxt.getSource(sb.toString());
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POMapCompExpression exp, TCType maptype, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new FiniteMapObligation(exp, maptype, choice));
		}
		
		return results;
	}
}
