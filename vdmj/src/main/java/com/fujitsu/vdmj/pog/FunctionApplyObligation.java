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
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.util.Utils;

public class FunctionApplyObligation extends ProofObligation
{
	public static final String UNKNOWN = "???";		// Use pre_(root, args) form

	private FunctionApplyObligation(POExpression root, POExpressionList args, String prename, POContextStack ctxt)
	{
		super(root.location, POType.FUNC_APPLY, ctxt);
		StringBuilder sb = new StringBuilder();

		if (prename == UNKNOWN)
		{
			sb.append("pre_(");
			sb.append(root);
			
			if (!args.isEmpty())
			{
				sb.append(", ");
				sb.append(Utils.listToString(args));
			}
			
			sb.append(")");
		}
		else
		{
			sb.append(prename);
			sb.append(Utils.listToString("(", args, ", ", ")"));
		}

		source = ctxt.getSource(sb.toString());
		setObligationVars(ctxt, args);
		setReasonsAbout(ctxt.getReasonsAbout());
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static ProofObligationList getAllPOs(POExpression root, POExpressionList args, String prename, POContextStack ctxt)
	{
		ProofObligationList results = new ProofObligationList();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new FunctionApplyObligation(root, args, prename, choice));
		}
		
		return results;
	}
}
