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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAssignmentContext;
import com.fujitsu.vdmj.pog.POContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;

public class POBlockStatement extends POSimpleBlockStatement
{
	private static final long serialVersionUID = 1L;

	public final PODefinitionList assignmentDefs;

	public POBlockStatement(LexLocation location, PODefinitionList assignmentDefs, POStatementList statements)
	{
		super(location, statements);
		this.assignmentDefs = assignmentDefs;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = assignmentDefs.getDefProofObligations(ctxt, pogState, env);
		
		if (!assignmentDefs.isEmpty())
		{
			POGState dclState = pogState.getLink();
			
			for (PODefinition dcl: assignmentDefs)
			{
				POAssignmentDefinition adef = (POAssignmentDefinition)dcl;
				dclState.addDclLocal(adef.name);
			}
	
			int dcls = ctxt.pushAt(new POAssignmentContext(assignmentDefs));
			obligations.addAll(super.getProofObligations(ctxt, dclState, env));
			
			// Remove the POAssignmentContext for the dcls, unless they are used in
			// "x := ..." assignments within the block, since the dcls are then needed
			// in any POs.
			
			boolean found = false;
			
			for (int sp = dcls + 1; sp < ctxt.size() && !found; sp++)
			{
				POContext item = ctxt.get(sp);
				
				if (item instanceof POAssignmentContext)
				{
					POAssignmentContext actxt = (POAssignmentContext)item;
					
					if (actxt.expression != null)	// <name> := <exp>
					{
						boolean local = false;

//						for (PODefinition dcl: assignmentDefs)
//						{
//							POAssignmentDefinition adef = (POAssignmentDefinition)dcl;
//							
//							if (adef.name.getName().equals(actxt.pattern))
//							{
//								local = true;
//								break;	// assignment to local dcl, so fine
//							}
//						}

						if (!local)
						{
							for (TCNameToken name: actxt.expression.readsState())
							{
								if (dclState.hasLocalName(name))
								{
									found = true;	// state assigned by dcl
									break;
								}
							}
						}
					}
				}
			}
			
			if (!found)		// dcl variables not used in assignments
			{
				ctxt.remove(dcls);
			}
			
			// The dclState goes out of scope here and its localUpdates have no further
			// effect. But updates made to locals in outer scopes will still be available.
		}
		else
		{
			obligations.addAll(super.getProofObligations(ctxt, pogState, env));
		}

		return obligations;
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBlockStatement(this, arg);
	}
}
