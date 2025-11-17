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

import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.POClassInvariantDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.expressions.POExpressionList;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.statements.POAssignmentStatement;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class StateInvariantObligation extends ProofObligation
{
	private StateInvariantObligation(POAssignmentStatement ass, POContextStack ctxt)
	{
		super(ass.location, POType.STATE_INVARIANT, ctxt);
		StringBuilder sb = new StringBuilder();
		
		if (ass.classDefinition != null)
		{
			PODefinitionList invdefs = ass.classDefinition.getInvDefs();
			POExpressionList vars = new POExpressionList();
			String sep = "";

			for (PODefinition d: invdefs)
			{
				POClassInvariantDefinition cid = (POClassInvariantDefinition)d;
				
				if (cid.location.module.equals(ass.location.module))
				{
					sb.append(sep);
					sb.append(cid.expression);
					vars.add(cid.expression);
					sep = " and ";
				}
			}

			// Obligation should cover the variables in its invariant
			setObligationVars(ctxt, vars);
			setReasonsAbout(ctxt.getReasonsAbout());
		}
		else	// must be because we have a module state invariant
		{
			POStateDefinition sdef = ass.stateDefinition;

			sb.append("let ");
			sb.append(sdef.invPattern);
			sb.append(" = ");
			sb.append(sdef.toPattern(true, sdef.location));	// maximal
			sb.append(" in\n");
			sb.append(sdef.invExpression);
			
			// Obligation should cover all state variables
			POExpressionList vars = new POExpressionList();
			vars.add(sdef.invExpression);
			
			for (TCNameToken svar: sdef.getVariableNames())
			{
				vars.add(new POVariableExpression(svar, null));
			}

			setObligationVars(ctxt, vars);
			setReasonsAbout(ctxt.getReasonsAbout());
		}

		source = ctxt.getSource(sb.toString());
	}

	public StateInvariantObligation(
		POClassInvariantDefinition def,
		POContextStack ctxt)
	{
		super(def.location, POType.STATE_INVARIANT, ctxt);
		StringBuilder sb = new StringBuilder();
		sb.append("After instance variable initializers\n");
		sb.append(invDefs(def.classDefinition));

    	source = ctxt.getSource(sb.toString());
    	markUnchecked(ProofObligation.UNCHECKED_VDMPP);
	}

	public StateInvariantObligation(
		POExplicitOperationDefinition def,
		POContextStack ctxt)
	{
		super(def.location, POType.STATE_INVARIANT, ctxt);
		StringBuilder sb = new StringBuilder();
		sb.append("After ");
		sb.append(def.name);
		sb.append(" constructor body\n");
		sb.append(invDefs(def.classDefinition));

    	source = ctxt.getSource(sb.toString());
    	markUnchecked(ProofObligation.UNCHECKED_VDMPP);
	}

	public StateInvariantObligation(
		POImplicitOperationDefinition def,
		POContextStack ctxt)
	{
		super(def.location, POType.STATE_INVARIANT, ctxt);
		StringBuilder sb = new StringBuilder();
		sb.append("After ");
		sb.append(def.name);
		sb.append(" constructor body\n");
		sb.append(invDefs(def.classDefinition));

    	source = ctxt.getSource(sb.toString());
    	markUnchecked(ProofObligation.UNCHECKED_VDMPP);
	}

	private String invDefs(POClassDefinition def)
	{
		StringBuilder sb = new StringBuilder();
		PODefinitionList invdefs = def.getInvDefs();
		String sep = "";

		for (PODefinition d: invdefs)
		{
			POClassInvariantDefinition cid = (POClassInvariantDefinition)d;
			sb.append(sep);
			sb.append(cid.expression);
			sep = " and ";
		}

    	return sb.toString();
	}
	
	/**
	 * Create an obligation for each of the alternative stacks contained in the ctxt.
	 * This happens with operation POs that push POAltContexts onto the stack.
	 */
	public static ProofObligationList getAllPOs(POAssignmentStatement ass, POContextStack ctxt)
	{
		ProofObligationList results = new ProofObligationList();
		
		for (POContextStack choice: ctxt.getAlternatives())
		{
			results.add(new StateInvariantObligation(ass, choice));
		}
		
		return results;
	}
}
