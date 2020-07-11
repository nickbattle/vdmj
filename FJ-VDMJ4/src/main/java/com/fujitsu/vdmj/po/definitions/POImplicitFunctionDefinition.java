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
import com.fujitsu.vdmj.po.types.POPatternListTypePair;
import com.fujitsu.vdmj.po.types.POPatternListTypePairList;
import com.fujitsu.vdmj.po.types.POPatternTypePair;
import com.fujitsu.vdmj.pog.FuncPostConditionObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POFunctionDefinitionContext;
import com.fujitsu.vdmj.pog.POFunctionResultContext;
import com.fujitsu.vdmj.pog.PONameContext;
import com.fujitsu.vdmj.pog.ParameterPatternObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SatisfiabilityObligation;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an implicit function definition.
 */
public class POImplicitFunctionDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	
	public final TCNameList typeParams;
	public final POPatternListTypePairList parameterPatterns;
	public final POPatternTypePair result;
	public final POExpression body;
	public final POExpression precondition;
	public final POExpression postcondition;
	public final TCFunctionType type;
	public final POExplicitFunctionDefinition predef;
	public final POExplicitFunctionDefinition postdef;
	public final boolean recursive;
	public final boolean isUndefined;
	public final TCType actualResult;
	public final POExplicitFunctionDefinition measureDef;
	public final TCNameToken measureName;

	public POImplicitFunctionDefinition(POAnnotationList annotations,
		TCNameToken name,
		TCNameList typeParams,
		POPatternListTypePairList parameterPatterns,
		POPatternTypePair result,
		POExpression body,
		POExpression precondition,
		POExpression postcondition,
		TCFunctionType type,
		POExplicitFunctionDefinition predef,
		POExplicitFunctionDefinition postdef,
		boolean recursive,
		boolean isUndefined,
		TCType actualResult,
		POExplicitFunctionDefinition measureDef,
		TCNameToken measureName)
	{
		super(name.getLocation(), name);

		this.annotations = annotations;
		this.typeParams = typeParams;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.type = type;
		this.predef = predef;
		this.postdef = postdef;
		this.recursive = recursive;
		this.isUndefined = isUndefined;
		this.measureDef = measureDef;
		this.actualResult = actualResult;
		this.measureName = measureName;
	}

	@Override
	public String toString()
	{
		return	name.getName() +
				(typeParams == null ? "" : "[" + typeParams + "]") +
				Utils.listToString("(", parameterPatterns, ", ", ")") + result +
				(body == null ? "" : " ==\n\t" + body) +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}

	@Override
	public TCType getType()
	{
		return type;		// NB overall "->" type, not result type
	}

	@Override
	public TCNameList getVariableNames()
	{
		return new TCNameList(name);
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt)
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

		if (precondition != null)
		{
			obligations.addAll(precondition.getProofObligations(ctxt));
		}

		if (postcondition != null)
		{
			if (body != null)	// else satisfiability, below
			{
				ctxt.push(new POFunctionDefinitionContext(this, false));
				obligations.add(new FuncPostConditionObligation(this, ctxt));
				ctxt.pop();
			}

			ctxt.push(new POFunctionResultContext(this));
			obligations.addAll(postcondition.getProofObligations(ctxt));
			ctxt.pop();
		}

		if (measureDef != null && measureName != null && measureName.getName().startsWith("measure_"))
		{
			ctxt.push(new PONameContext(new TCNameList(measureName)));
			obligations.addAll(measureDef.getProofObligations(ctxt));
			ctxt.pop();
		}

		if (body == null)
		{
			if (postcondition != null)
			{
				ctxt.push(new POFunctionDefinitionContext(this, false));
				obligations.add(new SatisfiabilityObligation(this, ctxt));
				ctxt.pop();
			}
		}
		else
		{
			ctxt.push(new POFunctionDefinitionContext(this, true));
    		obligations.addAll(body.getProofObligations(ctxt));

			if (isUndefined ||
				!TypeComparator.isSubType(actualResult, type.result))
			{
				obligations.add(new SubTypeObligation(
					this, type.result, actualResult, ctxt));
			}

			ctxt.pop();
		}

		if (annotations != null) annotations.poAfter(this, obligations, ctxt);
		return obligations;
	}

	public List<POPatternList> getParamPatternList()
	{
		List<POPatternList> list = new Vector<POPatternList>();
		
		for (POPatternListTypePair p: parameterPatterns)
		{
			list.add(p.patterns);
		}
		
		return list;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImplicitFunctionDefinition(this, arg);
	}
}
