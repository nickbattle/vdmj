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

import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INSubclassResponsibilityExpression;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.patterns.INPatternListList;
import com.fujitsu.vdmj.in.types.INInstantiate;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;

/**
 * A class to hold an explicit function definition.
 */
public class INExplicitFunctionDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameList typeParams;
	public final TCFunctionType type;
	public final INPatternListList paramPatternList;
	public final INExpression precondition;
	public final INExpression postcondition;
	public final INExpression body;
	public final boolean isTypeInvariant;
	public final TCNameToken measureName;
	public final INExplicitFunctionDefinition measureDef;
	public final boolean isCurried;
	public final INExplicitFunctionDefinition predef;
	public final INExplicitFunctionDefinition postdef;
	
	private Map<TCTypeList, FunctionValue> polyfuncs = null;

	public INExplicitFunctionDefinition(INAnnotationList annotations,
		INAccessSpecifier accessSpecifier, TCNameToken name,
		TCNameList typeParams, TCFunctionType type,
		INPatternListList parameters,
		INExpression body, INExpression precondition, INExpression postcondition,
		boolean typeInvariant, TCNameToken measureName, INExplicitFunctionDefinition measureDef,
		INExplicitFunctionDefinition predef, INExplicitFunctionDefinition postdef,
		INClassDefinition classdef)
	{
		super(name.getLocation(), accessSpecifier, name);

		this.annotations = annotations;
		this.typeParams = typeParams;
		this.type = type;
		this.paramPatternList = parameters;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.body = body;
		this.isTypeInvariant = typeInvariant;
		this.measureName = measureName;
		this.measureDef = measureDef;
		this.isCurried = parameters.size() > 1;
		this.predef = predef;
		this.postdef = postdef;
		this.classDefinition = classdef;

		type.instantiated = (typeParams == null) ? null : false;
	}

	@Override
	public String toString()
	{
		StringBuilder params = new StringBuilder();

		for (INPatternList plist: paramPatternList)
		{
			params.append("(" + Utils.listToString(plist) + ")");
		}

		return name.getName() +
				(typeParams == null ? ": " : "[" + typeParams + "]: ") + type +
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
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList nvl = new NameValuePairList();

		FunctionValue prefunc =
			(predef == null) ? null : new FunctionValue(predef, null, null, null);

		FunctionValue postfunc =
			(postdef == null) ? null : new FunctionValue(postdef, null, null, null);

		FunctionValue func = new FunctionValue(this, prefunc, postfunc, null);
		func.isStatic = accessSpecifier.isStatic;;
		func.uninstantiated = (typeParams != null);
		nvl.add(new NameValuePair(name, func));

		if (predef != null)
		{
			nvl.add(new NameValuePair(predef.name, prefunc));
			prefunc.uninstantiated = (typeParams != null);
		}

		if (postdef != null)
		{
			nvl.add(new NameValuePair(postdef.name, postfunc));
			postfunc.uninstantiated = (typeParams != null);
		}
		
		if (measureDef != null && measureDef.name.getName().startsWith("measure_"))
		{
			nvl.add(new NameValuePair(measureDef.name, new FunctionValue(measureDef, null, null, null)));
		}

		return nvl;
	}

	public FunctionValue getPolymorphicValue(TCTypeList argTypes, Context params, Context ctxt)
	{
		if (polyfuncs == null)
		{
			polyfuncs = new HashMap<TCTypeList, FunctionValue>();
		}
		else
		{
			// We always return the same function value for a polymorph
			// with a given set of types. This is so that the one function
			// value can record measure counts for recursive polymorphic
			// functions.
			
			FunctionValue rv = polyfuncs.get(argTypes);
			
			if (rv != null)
			{
				return rv;
			}
		}
		
		FunctionValue prefv = null;
		FunctionValue postfv = null;

		if (predef != null)
		{
			prefv = predef.getPolymorphicValue(argTypes, params, ctxt);
		}

		if (postdef != null)
		{
			postfv = postdef.getPolymorphicValue(argTypes, params, ctxt);
		}
		
		TCFunctionType ftype = (TCFunctionType)INInstantiate.instantiate(getType(), params, ctxt);
		FunctionValue rv = new FunctionValue(this, ftype, params, prefv, postfv, null);

		polyfuncs.put(argTypes, rv);
		return rv;
	}

	@Override
	public boolean isFunction()
	{
		return true;
	}

	@Override
	public boolean isCallableFunction()
	{
		return true;
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof INSubclassResponsibilityExpression;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExplicitFunctionDefinition(this, arg);
	}
}
