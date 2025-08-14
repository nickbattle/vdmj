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

package com.fujitsu.vdmj.po.statements;

import java.util.LinkedList;
import java.util.List;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.po.definitions.POAssignmentDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAssignmentContext;
import com.fujitsu.vdmj.pog.POContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.POReturnContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;

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
		ProofObligationList obligations = new ProofObligationList();
		
		PODefinitionList extracted = extractOpCalls(assignmentDefs, obligations, pogState, ctxt, env);
		extracted.getDefProofObligations(ctxt, pogState, env);
		
		if (!extracted.isEmpty())
		{
			POGState dclState = pogState.getLink();
			TCDefinitionList defs = new TCDefinitionList();
			
			for (PODefinition dcl: extracted)
			{
				POAssignmentDefinition adef = (POAssignmentDefinition)dcl;
				dclState.addDclLocal(adef.name);
				defs.add(new TCLocalDefinition(dcl.location, adef.name, adef.type));
			}

			Environment locals = new FlatEnvironment(defs, env);

			ctxt.pushAt(new POAssignmentContext(extracted));
			obligations.addAll(super.getProofObligations(ctxt, dclState, locals));
		}
		else
		{
			obligations.addAll(super.getProofObligations(ctxt, pogState, env));
		}

		return obligations;
	}

	/**
	 * Early attempt to clean up the context stack. But this became too complex with
	 * loops that include return paths.
	 */
	@SuppressWarnings("unused")
	private void cleanStack(POContextStack ctxt, POGState dclState, int dcls)
	{
		boolean keepLets = false;
		List<Integer> toDelete = new LinkedList<Integer>();
		
		if (!assignmentDefs.isEmpty())
		{
			toDelete.add(dcls);
		}
		
		for (int sp = dcls; sp < ctxt.size(); sp++)
		{
			POContext item = ctxt.get(sp);
			
			// handle POAltContext blocks here too?
			
			if (item instanceof POAssignmentContext)
			{
				POAssignmentContext actxt = (POAssignmentContext)item;
				
				if (actxt.expression != null)	// <name> := <exp>
				{
					boolean local = false;
					
					for (PODefinition dcl: assignmentDefs)
					{
						POAssignmentDefinition adef = (POAssignmentDefinition)dcl;
						
						if (adef.name.getName().equals(actxt.pattern))
						{
							toDelete.add(0, sp);	// Caused by x := to local x.
							local = true;
							break;
						}
					}

					if (!local)	// So assigning to outer state, sv := <expression>
					{
						for (TCNameToken name: actxt.expression.getVariableNames())
						{
							if (dclState.hasLocalName(name))
							{
								keepLets = true;	// state assigned using locals/dcls
								break;
							}
						}
					}
				}
			}
			else if (item instanceof POLetDefContext)	// A "let x = y in ..."
			{
				POLetDefContext lctxt = (POLetDefContext)item;
				toDelete.add(0, sp);
				
				for (PODefinition def: lctxt.localDefs)
				{
					if (def instanceof POValueDefinition)	// Ignore fn defs
					{
						POValueDefinition vdef = (POValueDefinition)def;
						
						// Add var name(s) to dclState, so visible above
						dclState.addDclLocal(vdef.pattern.getVariableNames());
					}
				}
			}
			else if (item instanceof POReturnContext)
			{
				POReturnContext rctxt = (POReturnContext)item;
				
				if (rctxt.result != null)
				{
					keepLets = true;	// Preserve stack for postcondition
				}
			}
		}
		
		if (!keepLets)				// dcl/let variables not used in state assignments
		{
			for (int sp: toDelete)	// Added in reverse order
			{
				ctxt.remove(sp);
			}
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBlockStatement(this, arg);
	}
}
