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

package com.fujitsu.vdmj.in.expressions;

import java.util.List;

import com.fujitsu.vdmj.in.definitions.INExplicitFunctionDefinition;
import com.fujitsu.vdmj.in.definitions.INImplicitFunctionDefinition;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.types.INInstantiate;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.visitors.TCParameterCollector;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.Value;

public class INFuncInstantiationExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression function;
	public final TCTypeList actualTypes;
	public final INExplicitFunctionDefinition expdef;
	public final INImplicitFunctionDefinition impdef;

	public INFuncInstantiationExpression(INExpression function, TCTypeList actualTypes,
		INExplicitFunctionDefinition expdef, INImplicitFunctionDefinition impdef)
	{
		super(function);
		this.function = function;
		this.actualTypes = actualTypes;
		this.expdef = expdef;
		this.impdef = impdef;
	}

	@Override
	public String toString()
	{
		return "(" + function + ")[" + Utils.listToString(actualTypes) + "]";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		FunctionValue fv = function.eval(ctxt).functionValue(ctxt);

    		if (!fv.uninstantiated)
    		{
    			abort(3034, "Function is already instantiated: " + fv.name, ctxt);
    		}

    		TCTypeList paramTypes = null;
    		
    		if (expdef != null)
    		{
    			paramTypes = expdef.typeParams;
    		}
    		else
    		{
    			paramTypes = impdef.typeParams;
    		}

    		Context params = new Context(location, "Instantiation params", null);
    		TCTypeList argtypes = new TCTypeList();
    		TCParameterCollector collector = new TCParameterCollector();

    		for (int i=0; i< actualTypes.size(); i++)
    		{
    			TCType ptype = actualTypes.get(i);
    			List<TCParameterType> names = ptype.apply(collector, null);
    			
    			if (!names.isEmpty())
    			{
    				// Resolve any @T types referred to in the type parameters
    				ptype = INInstantiate.instantiate(ptype, ctxt, ctxt);
    			}
    			
    			argtypes.add(ptype);
    			TCParameterType param = (TCParameterType) paramTypes.get(i);
    			params.put(param.name, new ParameterValue(ptype));
    		}
    		
    		FunctionValue rv = null;
    		
    		if (expdef != null)
			{
				rv = expdef.getPolymorphicValue(argtypes, params, ctxt);
			}
			else
			{
				rv = impdef.getPolymorphicValue(argtypes, params, ctxt);
			}

    		rv.setSelf(fv.self);
			rv.uninstantiated = false;
			return rv;
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFuncInstantiationExpression(this, arg);
	}
}
