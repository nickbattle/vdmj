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

import com.fujitsu.vdmj.po.annotations.POAnnotationList;
import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.patterns.POPattern;
import com.fujitsu.vdmj.po.patterns.POPatternList;
import com.fujitsu.vdmj.po.patterns.POPatternListList;
import com.fujitsu.vdmj.pog.FuncPostConditionObligation;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.POFunctionDefinitionContext;
import com.fujitsu.vdmj.pog.POFunctionResultContext;
import com.fujitsu.vdmj.pog.PONameContext;
import com.fujitsu.vdmj.pog.ParameterPatternObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.SubTypeObligation;
import com.fujitsu.vdmj.pog.TotalFunctionObligation;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A class to hold an explicit function definition.
 */
public class POExplicitFunctionDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	
	public final TCTypeList typeParams;
	public final TCFunctionType type;
	public final POPatternListList paramPatternList;
	public final POExpression precondition;
	public final POExpression postcondition;
	public final POExpression body;

	public final POExplicitFunctionDefinition predef;
	public final POExplicitFunctionDefinition postdef;
	public final PODefinitionListList paramDefinitionList;

	public final TCType expectedResult;
	public final TCType actualResult;
	public final boolean isUndefined;
	public final boolean recursive;
	public final POExplicitFunctionDefinition measureDef;
	public final TCNameToken measureName;

	public POExplicitFunctionDefinition(POAnnotationList annotations, TCNameToken name,
		TCTypeList typeParams, TCFunctionType type,
		POPatternListList parameters,
		POExpression body, POExpression precondition, POExpression postcondition,
		boolean isUndefined, TCType expectedResult, TCType actualResult,
		POExplicitFunctionDefinition predef,
		POExplicitFunctionDefinition postdef,
		PODefinitionListList paramDefinitionList,
		boolean recursive,
		POExplicitFunctionDefinition measureDef,
		TCNameToken measureName)
	{
		super(name.getLocation(), name);

		this.annotations = annotations;
		this.typeParams = typeParams;
		this.type = type;
		this.paramPatternList = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		this.expectedResult = expectedResult;
		this.actualResult = actualResult;
		this.isUndefined = isUndefined;
		this.measureDef = measureDef;
		this.predef = predef;
		this.postdef = postdef;
		this.paramDefinitionList = paramDefinitionList;
		this.recursive = recursive;
		this.measureName = measureName;
	}

	@Override
	public String toString()
	{
		StringBuilder params = new StringBuilder();

		for (POPatternList plist: paramPatternList)
		{
			params.append("(" + Utils.listToString(plist) + ")");
		}

		return name.getName() +
				(typeParams == null ? ": " : "[" + typeParams + "]: ") + Utils.deBracketed(type) +
				"\n\t" + name.getName() + params + " ==\n" + body +
				(precondition == null ? "" : "\n\tpre " + precondition) +
				(postcondition == null ? "" : "\n\tpost " + postcondition);
	}

	@Override
	public TCType getType()
	{
		return type;		// NB entire "->" type, not the result
	}

	@Override
	public ProofObligationList getProofObligations(POContextStack ctxt, Environment env)
	{
		ProofObligationList obligations =
				(annotations != null) ? annotations.poBefore(this, ctxt) : new ProofObligationList();
		TCNameList pids = new TCNameList();
		boolean matchNeeded = false;

		for (POPatternList pl: paramPatternList)
		{
			for (POPattern p: pl)
			{
				pids.addAll(p.getVariableNames());
			}
			
			if (!pl.alwaysMatches())
			{
				matchNeeded = true;
			}
		}
		
		if (type.hasTotal())
		{
			ctxt.push(new POFunctionDefinitionContext(this, true));
			obligations.add(new TotalFunctionObligation(this, ctxt));
			ctxt.pop();
		}

		if (pids.hasDuplicates() || matchNeeded)
		{
			obligations.add(new ParameterPatternObligation(this, ctxt));
		}

		if (precondition != null)
		{
			ctxt.push(new POFunctionDefinitionContext(this, false));
			obligations.addAll(precondition.getProofObligations(ctxt, env));
			ctxt.pop();
		}

		if (postcondition != null)
		{
			ctxt.push(new POFunctionDefinitionContext(this, false));
			obligations.add(new FuncPostConditionObligation(this, ctxt));
			ctxt.push(new POFunctionResultContext(this));
			obligations.addAll(postcondition.getProofObligations(ctxt, env));
			ctxt.pop();
			ctxt.pop();
		}
		
		if (measureDef != null && measureName != null && measureName.getName().startsWith("measure_"))
		{
			ctxt.push(new PONameContext(new TCNameList(measureName)));
			obligations.addAll(measureDef.getProofObligations(ctxt, env));
			ctxt.pop();
		}

		ctxt.push(new POFunctionDefinitionContext(this, true));
		obligations.addAll(body.getProofObligations(ctxt, env));

		if (isUndefined ||
			!TypeComparator.isSubType(actualResult, expectedResult))
		{
			obligations.add(
				new SubTypeObligation(this, expectedResult, actualResult, ctxt));
		}

		ctxt.pop();

		if (annotations != null) annotations.poAfter(this, obligations, ctxt);
		return obligations;
	}

	public List<POPatternList> getParamPatternList()
	{
		return paramPatternList;
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExplicitFunctionDefinition(this, arg);
	}
}
