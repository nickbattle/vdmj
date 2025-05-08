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

import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.statements.POErrorCase;

public class OperationPostConditionObligation extends ProofObligation
{
	public OperationPostConditionObligation(
		POExplicitOperationDefinition op, POContextStack ctxt)
	{
		super(op.postcondition.location, POType.OP_POST_CONDITION, ctxt);
		source = ctxt.getSource(getExp(op.precondition, op.postcondition, null));
		setObligationVars(ctxt, op.postcondition);
		setReasonsAbout(ctxt.getReasonsAbout());
	}

	public OperationPostConditionObligation(
		POImplicitOperationDefinition op, POContextStack ctxt)
	{
		super(op.postcondition.location, POType.OP_POST_CONDITION, ctxt);
		source = ctxt.getSource(getExp(op.precondition, op.postcondition, op.errors));
		setObligationVars(ctxt, op.postcondition);
		setReasonsAbout(ctxt.getReasonsAbout());
	}

	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static List<ProofObligation> getAllPOs(POExplicitOperationDefinition op, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives(false))	// NB! don't exclude returns
		{
			results.add(new OperationPostConditionObligation(op, choice));
		}
		
		return results;
	}

	public static List<ProofObligation> getAllPOs(POImplicitOperationDefinition op, POContextStack ctxt)
	{
		Vector<ProofObligation> results = new Vector<ProofObligation>();
		
		for (POContextStack choice: ctxt.getAlternatives(false))	// NB! don't exclude returns
		{
			results.add(new OperationPostConditionObligation(op, choice));
		}
		
		return results;
	}

	private String getExp(POExpression preexp, POExpression postexp, List<POErrorCase> errs)
	{
		if (errs == null)
		{
			return postexp.toString().replaceAll("~", "\\$");
		}
		else
		{
			StringBuilder sb = new StringBuilder();

			if (preexp != null)
			{
				sb.append("(");
				sb.append(preexp);
				sb.append(" and ");
				sb.append(postexp);
				sb.append(")");
			}
			else
			{
				sb.append(postexp);
			}

			for (POErrorCase err: errs)
			{
				sb.append(" or (");
				sb.append(err.left);
				sb.append(" and ");
				sb.append(err.right);
				sb.append(")");
			}

			return sb.toString().replaceAll("~", "\\$");
		}
	}
}
