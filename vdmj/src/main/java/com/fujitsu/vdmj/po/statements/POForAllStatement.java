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
import com.fujitsu.vdmj.po.annotations.POLoopInvariantAnnotation;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POLocalDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.LoopInvariantObligation;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllSequenceContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetBeStContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.typechecker.Environment;

public class POForAllStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POPattern pattern;
	public final POExpression set;
	public final POStatement statement;

	public POForAllStatement(LexLocation location,
		POPattern pattern, POExpression set, POStatement stmt)
	{
		super(location);
		this.pattern = pattern;
		this.set = set;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "for all " + pattern + " in set " + set + " do\n" + statement;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = set.getProofObligations(ctxt, pogState, env);

		POLoopInvariantAnnotation annotation = annotations.getInstance(POLoopInvariantAnnotation.class);
		TCNameSet updates = statement.updatesState();
		
		if (annotation == null)		// No loop invariant defined
		{
			int popto = ctxt.pushAt(new POForAllSequenceContext(pattern, set, " in set "));
			ProofObligationList loops = statement.getProofObligations(ctxt, pogState, env);
			ctxt.popTo(popto);
			
			if (!updates.isEmpty())
			{
				ctxt.push(new POAmbiguousContext("for all loop", updates, location));
			}
			
			obligations.addAll(loops);
			return obligations;
		}
		else
		{
			TCSetType stype = set.getExptype().getSet();
			PODefinitionList defs = pattern.getDefinitions(stype.setof);
			POLocalDefinition first = (POLocalDefinition) defs.firstElement();
			
			ctxt.push(new POLetBeStContext(first.name, "in set", set, null));
			obligations.addAll(LoopInvariantObligation.getAllPOs(annotation.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check initial for-loop");
			ctxt.pop();
			
			int popto = ctxt.size();
			
			ctxt.push(new POForAllSequenceContext(pattern, set, " in set "));
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check before for-loop");
			
			ctxt.push(new POImpliesContext(annotation.invariant));	// invariant => ...
			obligations.addAll(statement.getProofObligations(ctxt, pogState, env));
			
			obligations.addAll(LoopInvariantObligation.getAllPOs(statement.location, ctxt, annotation.invariant));
			obligations.lastElement().setMessage("check after for-loop");

			ctxt.popTo(popto);
			
			// Leave implication for following POs
			ctxt.push(new POImpliesContext(annotation.invariant));	// invariant => ...
			
			return obligations;
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForAllStatement(this, arg);
	}
}
