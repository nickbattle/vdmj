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

import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

/**
 * A class to represent assignable variable definitions.
 */
public class INAssignmentDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;

	public final TCType type;
	public final INExpression expression;
	public final TCType expType;

	public INAssignmentDefinition(INAccessSpecifier accessSpecifier,
			TCNameToken name, TCType type, INExpression expression, TCType expType)
	{
		super(name.getLocation(), accessSpecifier, name);
		this.type = type;
		this.expression = expression;
		this.expType = expType;
		this.location.executable(false);
	}

	@Override
	public String toString()
	{
		return name + ":" + type + " := " + expression;
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
        try
        {
	        Value v = expression.eval(ctxt);

	        if (!v.isUndefined())
	        {
	        	v = v.convertTo(type, ctxt);
	        }

			return new NameValuePairList(new NameValuePair(name, v.getUpdatable(null)));
        }
        catch (ValueException e)
        {
        	abort(e);
        	return null;
        }
 	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAssignmentDefinition(this, arg);
	}
}
