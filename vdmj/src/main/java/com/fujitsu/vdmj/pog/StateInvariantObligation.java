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

import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.POClassInvariantDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.statements.POAssignmentStatement;

public class StateInvariantObligation extends ProofObligation
{
	public StateInvariantObligation(POAssignmentStatement ass, POContextStack ctxt)
	{
		super(ass.location, POType.STATE_INVARIANT, ctxt);
		StringBuilder sb = new StringBuilder();
		
		// Note a StateDesignator is not a pattern, so this may not work!
		sb.append("let " + ass.target + " = " + ass.exp + " in\n");

		if (ass.classDefinition != null)
		{
			sb.append(invDefs(ass.classDefinition));
		}
		else	// must be because we have a module state invariant
		{
			POStateDefinition def = ass.stateDefinition;

			sb.append("let ");
			sb.append(def.invPattern);
			sb.append(" = ");
			sb.append(def.name);
			sb.append(" in ");
			sb.append(def.invExpression);
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
}
