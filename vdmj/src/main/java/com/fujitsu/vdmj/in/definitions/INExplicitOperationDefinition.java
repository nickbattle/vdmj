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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.INSubclassResponsibilityStatement;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.OperationValue;

/**
 * A class to hold an explicit operation definition.
 */
public class INExplicitOperationDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCOperationType type;
	public final INPatternList parameterPatterns;
	public final INExpression precondition;
	public final INExpression postcondition;
	public final INStatement body;
	public final boolean isConstructor;
	public final INExplicitFunctionDefinition predef;
	public final INExplicitFunctionDefinition postdef;
	public final INStateDefinition statedef;

	public INExplicitOperationDefinition(INAnnotationList annotations,
		INAccessSpecifier accessSpecifier,
		TCNameToken name, TCOperationType type,
		INPatternList parameters, INExpression precondition,
		INExpression postcondition, INStatement body, boolean isConstructor,
		INExplicitFunctionDefinition predef, INExplicitFunctionDefinition postdef,
		INStateDefinition statedef, INClassDefinition classdef)
	{
		super(name.getLocation(), accessSpecifier, name);

		this.annotations = annotations;
		this.type = type;
		this.parameterPatterns = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		this.isConstructor = isConstructor;
		this.predef = predef;
		this.postdef = postdef;
		this.statedef = statedef;
		
		this.classDefinition = classdef;
	}

	@Override
	public String toString()
	{
		return  accessSpecifier.ifSet(" ") + name + ": " + type +
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
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList nvl = new NameValuePairList();

		FunctionValue prefunc =
			(predef == null) ? null : new FunctionValue(predef, null, null, null, null);

		FunctionValue postfunc =
			(postdef == null) ? null : new FunctionValue(postdef, null, null, null, null);

		OperationValue op = new OperationValue(this, prefunc, postfunc, statedef);
		nvl.add(new NameValuePair(name, op));

		if (predef != null)
		{
			nvl.add(new NameValuePair(predef.name, prefunc));
		}

		if (postdef != null)
		{
			nvl.add(new NameValuePair(postdef.name, postfunc));
		}

		return nvl;
	}
	
	@Override
	public boolean isRuntime()
	{
		return !isSubclassResponsibility();
	}

	@Override
	public boolean isOperation()
	{
		return true;
	}

	@Override
	public boolean isCallableOperation()
	{
		return true;
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof INSubclassResponsibilityStatement;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExplicitOperationDefinition(this, arg);
	}
}
