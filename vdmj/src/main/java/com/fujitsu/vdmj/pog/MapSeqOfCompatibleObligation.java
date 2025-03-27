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

import com.fujitsu.vdmj.po.expressions.POMapEnumExpression;
import com.fujitsu.vdmj.po.expressions.POMapletExpression;

public class MapSeqOfCompatibleObligation extends ProofObligation
{
	private MapSeqOfCompatibleObligation(POMapEnumExpression exp, POContextStack ctxt)
	{
		super(exp.location, POType.MAP_SEQ_OF_COMPATIBLE, ctxt);
		StringBuilder sb = new StringBuilder();

		String m1 = getVar("m");
		String m2 = getVar("m");

		sb.append("forall " + m1 + ", " + m2 + " in set {");
		String prefix = "";

		for (POMapletExpression m: exp.members)
		{
			sb.append(prefix);
			sb.append("{");
			sb.append(m);
			sb.append("}");
			prefix = ", ";
		}

		String d1 = getVar("d");
		String d2 = getVar("d");

		sb.append("} &\n  forall " + d1 + " in set dom " + m1 + ", " + d2 + " in set dom " + m2 + " &\n");
		sb.append("    " + d1 + " = " + d2 + " => " + m1 + "(" + d1 + ") = " + m2 + "(" + d2 + ")");

		source = ctxt.getSource(sb.toString());
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POMapEnumExpression exp, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new MapSeqOfCompatibleObligation(exp, choice));
		}
		
		return results;
	}
}
