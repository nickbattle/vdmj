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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.statements.INErrorCase;
import com.fujitsu.vdmj.in.statements.INErrorCaseList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.INSubclassResponsibilityStatement;
import com.fujitsu.vdmj.in.types.INPatternListTypePair;
import com.fujitsu.vdmj.in.types.INPatternListTypePairList;
import com.fujitsu.vdmj.in.types.INPatternTypePair;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.OperationValue;

/**
 * A class to hold an explicit operation definition.
 */
public class INImplicitOperationDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INPatternListTypePairList parameterPatterns;
	public final INPatternTypePair result;
	public final INStatement body;
	public final INExpression precondition;
	public final INExpression postcondition;
	public final INErrorCaseList errors;
	public final boolean isConstructor;
	public final INExplicitFunctionDefinition predef;
	public final INExplicitFunctionDefinition postdef;
	public final INStateDefinition statedef;
	public final TCOperationType type;		// Created from params/result

	public INImplicitOperationDefinition(INAnnotationList annotations,
		INAccessSpecifier accessSpecifier, TCNameToken name,
		INPatternListTypePairList parameterPatterns,
		INPatternTypePair result, INStatement body,
		INExpression precondition, INExpression postcondition, INErrorCaseList errors, boolean isConstructor,
		INExplicitFunctionDefinition predef, INExplicitFunctionDefinition postdef, INStateDefinition statedef,
		INClassDefinition classdef)
	{
		super(name.getLocation(), accessSpecifier, name);

		this.annotations = annotations;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.errors = errors;
		this.isConstructor = isConstructor;
		this.predef = predef;
		this.postdef = postdef;
		this.statedef = statedef;
		this.classDefinition = classdef;

		TCTypeList ptypes = new TCTypeList();

		for (INPatternListTypePair ptp: parameterPatterns)
		{
			ptypes.addAll(ptp.getTypeList());
		}

		type = new TCOperationType(location, ptypes,
					(result == null ? new TCVoidType(name.getLocation()) : result.type));
		
		type.setPure(accessSpecifier.isPure);
	}

	@Override
	public String toString()
	{
		return	(type.isPure() ? "pure " : "") + name +
				Utils.listToString("(", parameterPatterns, ", ", ")") +
				(result == null ? "" : " " + result) +
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
	public INExpression findExpression(int lineno)
	{
		if (predef != null)
		{
			INExpression found = predef.findExpression(lineno);
			if (found != null) return found;
		}

		if (postdef != null)
		{
			INExpression found = postdef.findExpression(lineno);
			if (found != null) return found;
		}
		
		if (errors != null)
		{
			for (INErrorCase err: errors)
			{
				INExpression found = err.findExpression(lineno);
				if (found != null) return found;
			}
		}

		return body == null ? null : body.findExpression(lineno);
	}

	@Override
	public INStatement findStatement(int lineno)
	{
		return body == null ? null : body.findStatement(lineno);
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList nvl = new NameValuePairList();

		FunctionValue prefunc =
			(predef == null) ? null : new FunctionValue(predef, null, null, null);

		FunctionValue postfunc =
			(postdef == null) ? null : new FunctionValue(postdef, null, null, null);

		// Note, body may be null if it is really implicit. This is caught
		// when the function is invoked. The value is needed to implement
		// the pre_() expression for implicit functions.

		OperationValue op =	new OperationValue(this, prefunc, postfunc, statedef);
		op.isConstructor = isConstructor;
		op.isStatic = accessSpecifier.isStatic;
		nvl.add(new NameValuePair(name, op));

		if (predef != null)
		{
			prefunc.isStatic = accessSpecifier.isStatic;
			nvl.add(new NameValuePair(predef.name, prefunc));
		}

		if (postdef != null)
		{
			postfunc.isStatic = accessSpecifier.isStatic;
			nvl.add(new NameValuePair(postdef.name, postfunc));
		}

		return nvl;
	}

	public INPatternList getParamPatternList()
	{
		INPatternList plist = new INPatternList();

		for (INPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		return plist;
	}

	@Override
	public boolean isOperation()
	{
		return true;
	}

	@Override
	public boolean isCallableOperation()
	{
		return (body != null);
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof INSubclassResponsibilityStatement;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImplicitOperationDefinition(this, arg);
	}
}
