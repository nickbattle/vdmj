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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.POStateDefinition;
import com.fujitsu.vdmj.po.definitions.POValueDefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.po.patterns.POIdentifierPattern;
import com.fujitsu.vdmj.po.statements.visitors.POStatementVisitor;
import com.fujitsu.vdmj.pog.POAmbiguousContext;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POForAllContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POLetDefContext;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SatisfiabilityObligation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;

public class POSpecificationStatement extends POStatement
{
	private static final long serialVersionUID = 1L;
	public final POExternalClauseList externals;
	public final POExpression precondition;
	public final POExpression postcondition;
	public final POErrorCaseList errors;
	public final POStateDefinition stateDefinition;

	public POSpecificationStatement(LexLocation location,
		POExternalClauseList externals, POExpression precondition,
		POExpression postcondition, POErrorCaseList errors,
		POStateDefinition stateDefinition)
	{
		super(location);

		this.externals = externals;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;
		this.stateDefinition = stateDefinition;
	}

	@Override
	public String toString()
	{
		return "[" +
    		(externals == null ? "" : "\n\text " + externals) +
    		(precondition == null ? "" : "\n\tpre " + precondition) +
    		(postcondition == null ? "" : "\n\tpost " + postcondition) +
    		(errors == null ? "" : "\n\terrs " + errors) + "]";
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, POGState pogState, Environment env)
	{
		ProofObligationList obligations = new ProofObligationList();
		TCNameList writeList = new TCNameList();

		if (externals != null)
		{
			for (POExternalClause ext: externals)
			{
				if (ext.mode.is(Token.WRITE))
				{
					writeList.addAll(ext.identifiers);
				}
			}
		}

		if (errors != null)
		{
			for (POErrorCase err: errors)
			{
				obligations.addAll(err.left.getProofObligations(ctxt, pogState, env));
				obligations.addAll(err.right.getProofObligations(ctxt, pogState, env));
			}
		}

		if (precondition != null)
		{
			obligations.addAll(precondition.getProofObligations(ctxt, pogState, env));
		}

		obligations.addAll(postcondition.getProofObligations(ctxt, pogState, env));
		
		if (Settings.dialect == Dialect.VDM_SL)
		{
			obligations.add(new SatisfiabilityObligation(this, stateDefinition, ctxt, env));
		}

		if (errors != null)
		{
			ctxt.push(new POAmbiguousContext("statement throws exceptions", ctxt.getStateVariables(), location));
		}
		else if (!writeList.isEmpty())
		{
			addOldContext(ctxt);
			ctxt.push(new POForAllContext(writeList, env));		// forall <changed variables>
			ctxt.push(new POImpliesContext(postcondition));		// post => ...
		}
		else
		{
			addOldContext(ctxt);
			ctxt.push(new POForAllContext(ctxt.getStateVariables(), env));		// forall <changed variables>
			ctxt.push(new POImpliesContext(postcondition));						// post => ...
		}

		return obligations;
	}

	private void addOldContext(POContextStack ctxt)
	{
		if (postcondition != null)
		{
			PODefinitionList olddefs = new PODefinitionList();
			
			for (TCNameToken name: postcondition.getVariableNames())
			{
				if (name.isOld())
				{
					TCNameToken varname = new TCNameToken(name.getLocation(), name.getModule(), name.getName() + "$");
					
					olddefs.add(new POValueDefinition(null, new POIdentifierPattern(varname), null,
							new POVariableExpression(name.getNewName(), null), null, null));
				}
			}
			
			if (!olddefs.isEmpty())
			{
				ctxt.push(new POLetDefContext(olddefs));
			}
		}
	}

	@Override
	public <R, S> R apply(POStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseSpecificationStatement(this, arg);
	}
}
