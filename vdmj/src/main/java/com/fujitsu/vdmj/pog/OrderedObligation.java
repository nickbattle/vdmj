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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.pog;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class OrderedObligation extends ProofObligation
{
	private OrderedObligation(POExpression left, POExpression right, TCTypeSet types, POContextStack ctxt)
	{
		super(left.location, POType.ORDERED, ctxt);
		StringBuilder sb = new StringBuilder();
		String prefix = "";

		for (TCType type: types)
		{
			if (!TypeComparator.isSubType(left.getExptype(), type))
			{
				sb.append(prefix);
	    		sb.append("is_(");
	    		sb.append(left);
	    		sb.append(", ");
	    		sb.append(explicitType(type, left.location));
	    		sb.append(")");
	    		prefix = " and ";
			}
			
			if (!TypeComparator.isSubType(right.getExptype(), type))
			{
				sb.append(prefix);
		    	sb.append("is_(");
	    		sb.append(right);
	    		sb.append(", ");
	    		sb.append(explicitType(type, right.location));
	    		sb.append(")");
	    		prefix = " or ";
			}
		}
		
		source = ctxt.getSource(sb.toString());
		setObligationVars(ctxt, left, right);
		setReasonsAbout(ctxt.getReasonsAbout());
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POExpression left, POExpression right, TCTypeSet types, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new OrderedObligation(left, right, types, choice));
		}
		
		return results;
	}
}
