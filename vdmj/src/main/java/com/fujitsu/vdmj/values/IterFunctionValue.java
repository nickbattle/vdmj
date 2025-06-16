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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.values;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.values.visitors.ValueVisitor;

public class IterFunctionValue extends FunctionValue
{
	private static final long serialVersionUID = 1L;
	public final FunctionValue function;
	public final long iterations;

	public IterFunctionValue(FunctionValue function, long iterations)
	{
		super(function.location, function.type, "**", function.classdef);
		this.function = function;
		this.iterations = iterations;
	}

	@Override
	public String toString()
	{
		return function.toString() + " ** " + iterations;
	}

	@Override
	public Value eval(
		LexLocation from, ValueList argValues, Context ctxt) throws ValueException
	{
		Value result = function.eval(from, argValues, ctxt);

		for (int i=1; i<iterations; i++)
		{
			result = function.eval(from, result, ctxt);
		}

		return result;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof Value)
		{
			Value val = ((Value)other).deref();

			if (val instanceof IterFunctionValue)
    		{
    			IterFunctionValue ov = (IterFunctionValue)val;
    			return ov.function.equals(function) && ov.iterations == iterations;
    		}
		}

		return false;
	}
	
	@Override
	protected Value convertValueTo(TCType to, Context ctxt, TCTypeSet done) throws ValueException
	{
		FunctionValue converted = (FunctionValue)function.convertValueTo(to, ctxt, done);
		return new IterFunctionValue(converted, iterations);
	}

	@Override
	public int hashCode()
	{
		return function.hashCode() + (int)iterations;
	}

	@Override
	public String kind()
	{
		return "**";
	}

	@Override
	public <R, S> R apply(ValueVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIterFunctionValue(this, arg);
	}
}
