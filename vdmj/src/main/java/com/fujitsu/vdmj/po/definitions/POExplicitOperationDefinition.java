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

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.statements.POStatement;
import com.fujitsu.vdmj.pog.OperationPostConditionObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POFunctionDefinitionContext;
import com.fujitsu.vdmj.pog.POImpliesContext;
import com.fujitsu.vdmj.pog.PONoCheckContext;
import com.fujitsu.vdmj.pog.ParameterPatternObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
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
public class POExplicitOperationDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final TCOperationType type;
	public final POPatternList parameterPatterns;
	public final POExpression precondition;
	public final POExpression postcondition;
	public final POStatement body;
	public final POExplicitFunctionDefinition predef;
	public final POExplicitFunctionDefinition postdef;
	public final PODefinitionList paramDefinitions;
	public final POStateDefinition state;
	public final TCType actualResult;
	public final boolean isConstructor;

	public POExplicitOperationDefinition(POAnnotationList annotations,
		TCNameToken name, TCOperationType type,
		POPatternList parameters, POExpression precondition,
		POExpression postcondition, POStatement body,
		POExplicitFunctionDefinition predef,
		POExplicitFunctionDefinition postdef,
		PODefinitionList paramDefinitions,
		POStateDefinition state,
		TCType actualResult, boolean isConstructor)
	{
		super(name.getLocation(), name);

		this.annotations = annotations;
		this.type = type;
		this.parameterPatterns = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		this.predef = predef;
		this.postdef = postdef;
		this.paramDefinitions = paramDefinitions;
		this.state = state;
		this.actualResult = actualResult;
		this.isConstructor = isConstructor;
	}

	@Override
	public String toString()
	{
		return  (type.isPure() ? "pure " : "") + name + ": " + type +
				"\n\t" + name + "(" + Utils.listToString(parameterPatterns) + ")" +
				(body == null ? "" : " ==\n" + body) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}

	@Override
	public TCType getType()
	{
		return type;		// NB entire "==>" type, not result
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations =
				(annotations != null) ? annotations.poBefore(this, ctxt) : new ProofObligationList();
		TCNameList pids = new TCNameList();

		for (POPattern p: parameterPatterns)
		{
			pids.addAll(p.getVariableNames());
		}

		if (pids.hasDuplicates() || !parameterPatterns.alwaysMatches())
		{
			obligations.add(new ParameterPatternObligation(this, ctxt));
		}

		if (precondition != null)
		{
			obligations.addAll(predef.getProofObligations(ctxt, env));
		}

		if (postcondition != null)
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

			ctxt.push(new PONoCheckContext());
			obligations.add(new OperationPostConditionObligation(this, ctxt));
			ctxt.pop();
		}

		ctxt.push(new PONoCheckContext());
		obligations.addAll(body.getProofObligations(ctxt, env));

		if (isConstructor &&
			classDefinition != null &&
			classDefinition.invariant != null)
		{
			obligations.add(new StateInvariantObligation(this, ctxt));
		}

		if (!isConstructor &&
			!TypeComparator.isSubType(actualResult, type.result))
		{
			obligations.add(new SubTypeObligation(this, actualResult, ctxt));
		}
		
		ctxt.pop();

		if (annotations != null) annotations.poAfter(this, obligations, ctxt);
		return obligations;
	}

	public List<POPatternList> getParamPatternList()
	{
		List<POPatternList> list = new Vector<POPatternList>();
		list.add(parameterPatterns);
		return list;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExplicitOperationDefinition(this, arg);
	}
}
