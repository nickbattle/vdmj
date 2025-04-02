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

import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.POStateDesignator;

public class SeqApplyObligation extends ProofObligation
{
	private SeqApplyObligation(POExpression root, POExpression poExpression, POContextStack ctxt)
	{
		super(root.location, POType.SEQ_APPLY, ctxt);
		source = ctxt.getSource(poExpression + " in set inds " + root);
		setObligationVars(ctxt, root, poExpression);
		// Note, the LHS included, eg. "len s in set inds s"
		setReasonsAbout(ctxt.getReasonsAbout(), poExpression.getVariableNames());
	}

	private SeqApplyObligation(POStateDesignator root, POExpression arg, POContextStack ctxt)
	{
		super(root.location, POType.SEQ_APPLY, ctxt);
		source = ctxt.getSource(arg + " in set inds " + root);
		setObligationVars(ctxt, root.getExpressions());
		setReasonsAbout(ctxt.getReasonsAbout());
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POExpression root, POExpression poExpression, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new SeqApplyObligation(root, poExpression, choice));
		}
		
		return results;
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POStateDesignator root, POExpression arg, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new SeqApplyObligation(root, arg, choice));
		}
		
		return results;
	}
}
