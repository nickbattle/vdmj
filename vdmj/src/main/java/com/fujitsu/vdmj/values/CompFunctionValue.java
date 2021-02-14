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

package com.fujitsu.vdmj.values;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class CompFunctionValue extends FunctionValue
{
	private static final long serialVersionUID = 1L;
	public final FunctionValue ff1;
	public final FunctionValue ff2;

	public CompFunctionValue(FunctionValue f1, FunctionValue f2)
	{
		super(f1.location,
			new TCFunctionType(f1.location,f2.type.parameters, 
				f1.type.partial || f2.type.partial, f1.type.result), "comp", f1.classdef);
		this.ff1 = f1;
		this.ff2 = f2;
	}

	@Override
	public String toString()
	{
		return ff2.type.parameters + " -> " + ff1.type.result;
	}

	@Override
	public Value eval(
		LexLocation from, ValueList argValues, Context ctxt) throws ValueException
	{
		ValueList f1arg = new ValueList();
		f1arg.add(ff2.eval(from, argValues, ctxt));
		return ff1.eval(from, f1arg, ctxt);
	}

	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		if (to.isFunction(location))
		{
			if (type.equals(to) || to.isUnknown(location))
			{
				return this;
			}
			else
			{
				TCFunctionType restrictedType = to.getFunction();
				
				if (type.equals(restrictedType))
				{
					return this;
				}
				else if (TypeComparator.isSubType(type, restrictedType))
				{
					// The new function amends the parameters of ff2 and the result of ff1
					
					FunctionValue leftValue = (FunctionValue)ff1.clone();
					leftValue.type = new TCFunctionType(ff1.location,
						ff1.type.parameters, ff1.type.partial, restrictedType.result);
					
					FunctionValue rightValue = (FunctionValue)ff2.clone();
					rightValue.type = new TCFunctionType(ff2.location,
						restrictedType.parameters, ff2.type.partial, ff2.type.result);
					
					return new CompFunctionValue(leftValue, rightValue);
				}
				else
				{
					return abort(4165, "Cannot convert " + this + " to " + restrictedType, ctxt);
				}
			}
		}
		else
		{
			return super.convertValueTo(to, ctxt, done);
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

    		if (val instanceof CompFunctionValue)
    		{
    			CompFunctionValue ov = (CompFunctionValue)val;
    			return ov.ff1.equals(ff1) && ov.ff2.equals(ff2);
    		}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return ff1.hashCode() + ff2.hashCode();
	}

	@Override
	public String kind()
	{
		return "comp";
	}

	@Override
	public Object clone()
	{
		return new CompFunctionValue((FunctionValue)ff1.clone(), (FunctionValue)ff2.clone());
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCompFunctionValue(this, arg);
	}
}
