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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POPatternListList;
import com.fujitsu.vdmj.po.statements.POErrorCaseList;
import com.fujitsu.vdmj.po.statements.POExternalClauseList;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.po.types.POPatternListTypePairList;
import com.fujitsu.vdmj.po.types.POPatternTypePair;
import com.fujitsu.vdmj.pog.OperationPostConditionObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POFunctionDefinitionContext;
import com.fujitsu.vdmj.pog.POGState;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.POOperationDefinitionContext;
import com.fujitsu.vdmj.pog.ParameterPatternObligation;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SatisfiabilityObligation;
import com.fujitsu.vdmj.pog.StateInvariantObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.pog.TotalFunctionObligation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an explicit operation definition.
 */
public class POImplicitOperationDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final POPatternListTypePairList parameterPatterns;
	public final POPatternTypePair result;
	public final POExternalClauseList externals;
	public final POStatement body;
	public final POExpression precondition;
	public final POExpression postcondition;
	public final POErrorCaseList errors;

	public final TCOperationType type;
	public final POExplicitFunctionDefinition predef;
	public final POExplicitFunctionDefinition postdef;
	public final TCType actualResult;
	public final PODefinition stateDefinition;
	public final boolean isConstructor;

	public POImplicitOperationDefinition(POAnnotationList annotations,
		TCNameToken name,
		POPatternListTypePairList parameterPatterns,
		POPatternTypePair result, POStatement body,
		POExternalClauseList externals,
		POExpression precondition,
		POExpression postcondition,
		POErrorCaseList errors,
		TCOperationType type,
		POExplicitFunctionDefinition predef,
		POExplicitFunctionDefinition postdef,
		TCType actualResult,
		POStateDefinition stateDefinition,
		boolean isConstructor)
	{
		super(name.getLocation(), name);
		
		this.annotations = annotations;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.externals = externals;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;
		this.type = type;
		this.predef = predef;
		this.postdef = postdef;
		this.actualResult = actualResult;
		this.stateDefinition = stateDefinition;
		this.isConstructor = isConstructor;
	}

	@Override
	public String toString()
	{
		return	(type.isPure() ? "pure " : "") + name +
				Utils.listToString("(", parameterPatterns, ", ", ")") +
				(result == null ? "" : " " + result) +
				(externals == null ? "" : "\n\text " + externals) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition) +
				(errors == null ? "" : "\n\terrs " + errors);
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations =
				(annotations != null) ? annotations.poBefore(this, ctxt) : new ProofObligationList();
		TCNameList pids = new TCNameList();
		boolean matchNeeded = false;

		for (POPatternListTypePair pltp: parameterPatterns)
		{
			for (POPattern p: pltp.patterns)
			{
				pids.addAll(p.getVariableNames());
			}
			
			if (!pltp.patterns.alwaysMatches())
			{
				matchNeeded = true;
			}
		}

		if (pids.hasDuplicates() || matchNeeded)
		{
			obligations.add(new ParameterPatternObligation(this, ctxt));
		}

		/**
		 * Pre- and postconditions for OO dialects are not clearly defined, and obligations
		 * generated from them are generally not type-checkable. So we conditionally
		 * exclude them, depending on the dialect.
		 */
		if (precondition != null && Settings.dialect == Dialect.VDM_SL)
		{
			obligations.addAll(predef.getProofObligations(ctxt, env));
		}

		if (postcondition != null && Settings.dialect == Dialect.VDM_SL)
		{
			if (precondition != null)
			{
				ctxt.push(new POFunctionDefinitionContext(postdef, true));
				ctxt.push(new POImpliesContext(this));
				obligations.add(new TotalFunctionObligation(postdef, ctxt));
				ctxt.pop();
				ctxt.pop();
			}
			else
			{
				obligations.addAll(postdef.getProofObligations(ctxt, env));
			}
			
			obligations.add(new OperationPostConditionObligation(this, ctxt));
		}
		
		if (body != null)
		{
			POGState pstate = new POGState();
			
			if (stateDefinition != null)
			{
				ctxt.push(new POOperationDefinitionContext(this, (precondition != null), stateDefinition, true));
				obligations.addAll(body.getProofObligations(ctxt, pstate, env));
				ctxt.pop();
			}
			else if (classDefinition != null)
			{
				ctxt.push(new POOperationDefinitionContext(this, (precondition != null), classDefinition, true));
				ProofObligationList oblist = body.getProofObligations(ctxt, pstate, env);
				ctxt.pop();
				
				if (Settings.release != Release.VDM_10)		// Uses the obj_C pattern in OperationDefContext
				{
					oblist.markUnchecked(ProofObligation.REQUIRES_VDM10);
				}
				else if (precondition != null)				// pre_op state param undefined in VDM++
				{
					oblist.markUnchecked(ProofObligation.UNCHECKED_VDMPP);
				}
					
				obligations.addAll(oblist);
			}
			else	// Flat spec with no state defined
			{
				ctxt.push(new POOperationDefinitionContext(this, (precondition != null), null, true));
				obligations.addAll(body.getProofObligations(ctxt, pstate, env));
				ctxt.pop();
			}

			if (isConstructor &&
				classDefinition != null &&
				classDefinition.invariant != null)
			{
				obligations.add(new StateInvariantObligation(this, ctxt));
			}

			if (!isConstructor &&
				!TypeComparator.isSubType(actualResult, type.result))
			{
				obligations.add(new SubTypeObligation(this, actualResult, ctxt).
					markUnchecked(ProofObligation.NOT_YET_SUPPORTED));
			}
		}
		else
		{
			if (postcondition != null && Settings.dialect == Dialect.VDM_SL &&
				Settings.release == Release.VDM_10)		// Uses obj_C pattern
			{
				ctxt.push(new POOperationDefinitionContext(this, false, stateDefinition, false));
				obligations.add(new SatisfiabilityObligation(this, stateDefinition, ctxt));
				ctxt.pop();
			}
		}

		if (annotations != null) annotations.poAfter(this, obligations, ctxt);
		return obligations;
	}

	public POPatternListList getListParamPatternList()
	{
		POPatternListList list = new POPatternListList();
		
		for (POPatternListTypePair p: parameterPatterns)
		{
			list.add(p.patterns);
		}
		
		return list;
	}
	
	public POPatternList getParamPatternList()
	{
		POPatternList plist = new POPatternList();

		for (POPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		return plist;
	}

	@Override
	public boolean updatesState()
	{
		return body == null ? false : body.updatesState();
	}
	
	@Override
	public boolean readsState()
	{
		return body == null ? false : body.readsState();
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImplicitOperationDefinition(this, arg);
	}
}
