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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.annotations.INAnnotationList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INSubclassResponsibilityExpression;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.types.INPatternListTypePair;
import com.fujitsu.vdmj.in.types.INPatternListTypePairList;
import com.fujitsu.vdmj.in.types.INPatternTypePair;
import com.fujitsu.vdmj.in.types.Instantiate;
import com.fujitsu.vdmj.lex.Dialect;
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
 * A class to hold an implicit function definition.
 */
public class INImplicitFunctionDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameList typeParams;
	public final INPatternListTypePairList parameterPatterns;
	public final INPatternTypePair result;
	public final INExpression body;
	public final INExpression precondition;
	public final INExpression postcondition;
	public final TCNameToken measureName;
	public final INExpression measureExp;
	public final INExplicitFunctionDefinition measureDef;
	public final INExplicitFunctionDefinition predef;
	public final INExplicitFunctionDefinition postdef;
	public final INClassDefinition classdef;
	public final TCFunctionType type;

	private Map<TCTypeList, FunctionValue> polyfuncs = null;

	public INImplicitFunctionDefinition(INAnnotationList annotations,
		INAccessSpecifier accessSpecifier, TCNameToken name,
		TCNameList typeParams,
		INPatternListTypePairList parameterPatterns,
		INPatternTypePair result,
		INExpression body,
		INExpression precondition,
		INExpression postcondition, INExpression measureExp, TCNameToken measureName, INExplicitFunctionDefinition measureDef,
		INExplicitFunctionDefinition predef, INExplicitFunctionDefinition postdef,
		INClassDefinition classdef)
	{
		super(name.getLocation(), accessSpecifier, name);

		this.annotations = annotations;
		this.typeParams = typeParams;
		this.parameterPatterns = parameterPatterns;
		this.result = result;
		this.body = body;
		this.precondition = precondition;
		this.postcondition = postcondition;
		this.measureName = measureName;
		this.measureExp = measureExp;
		this.measureDef = measureDef;
		this.predef = predef;
		this.postdef = postdef;
		this.classdef = classdef;

		TCTypeList ptypes = new TCTypeList();

		for (INPatternListTypePair ptp: parameterPatterns)
		{
			ptypes.addAll(ptp.getTypeList());
		}

		// NB: implicit functions are always +> total, apparently
		type = new TCFunctionType(location, ptypes, false, result.type);
		type.instantiated = (typeParams == null) ? null : false;
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

		return body == null ? null : body.findExpression(lineno);
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList nvl = new NameValuePairList();
		Context free = ctxt.getVisibleVariables();

		FunctionValue prefunc =
			(predef == null) ? null : new FunctionValue(predef, null, null, free);

		FunctionValue postfunc =
			(postdef == null) ? null : new FunctionValue(postdef, null, null, free);

		// Note, body may be null if it is really implicit. This is caught
		// when the function is invoked. The value is needed to implement
		// the pre_() expression for implicit functions.

		FunctionValue func = new FunctionValue(this, prefunc, postfunc, free);
		func.isStatic = accessSpecifier.isStatic;
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

		if (Settings.dialect == Dialect.VDM_SL)
		{
			// This is needed for recursive local functions
			free.putList(nvl);
		}

		return nvl;
	}

	public FunctionValue getPolymorphicValue(TCTypeList actualTypes, Context params, Context ctxt)
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
			
			FunctionValue rv = polyfuncs.get(actualTypes);
			
			if (rv != null)
			{
				return rv;
			}
		}
		
		FunctionValue prefv = null;
		FunctionValue postfv = null;

		if (predef != null)
		{
			prefv = predef.getPolymorphicValue(actualTypes, params, ctxt);
		}

		if (postdef != null)
		{
			postfv = postdef.getPolymorphicValue(actualTypes, params, ctxt);
		}

		TCFunctionType ftype = (TCFunctionType)Instantiate.instantiate(getType(), params, ctxt);
		FunctionValue rv = new FunctionValue(this, ftype, params, prefv, postfv, null);

		polyfuncs.put(actualTypes, rv);
		return rv;
	}

	public List<INPatternList> getParamPatternList()
	{
		List<INPatternList> parameters = new Vector<INPatternList>();
		INPatternList plist = new INPatternList();

		for (INPatternListTypePair pl: parameterPatterns)
		{
			plist.addAll(pl.patterns);
		}

		parameters.add(plist);
		return parameters;
	}

	@Override
	public boolean isFunction()
	{
		return true;
	}

	@Override
	public boolean isCallableFunction()
	{
		return (body != null);
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return body instanceof INSubclassResponsibilityExpression;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseImplicitFunctionDefinition(this, arg);
	}
}
