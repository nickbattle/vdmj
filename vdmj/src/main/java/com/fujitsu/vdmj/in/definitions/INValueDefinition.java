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
import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.Value;

/**
 * A class to hold a value definition.
 */
public class INValueDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INPattern pattern;
	public final TCType type;
	public final INExpression exp;

	public INValueDefinition(INAnnotationList annotations,
		INAccessSpecifier accessSpecifier, TCNameToken name, INPattern p, TCType type, INExpression exp)
	{
		super(p.location, accessSpecifier, name);

		this.annotations = annotations;
		this.pattern = p;
		this.type = type;
		this.exp = exp;
	}

	@Override
	public String toString()
	{
		return pattern + (type == null ? "" : ":" + type) + " = " + exp;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof INValueDefinition)
		{
			INValueDefinition vdo = (INValueDefinition)other;
			return pattern.equals(vdo.pattern) && type.equals(vdo.type) && exp.equals(vdo.exp);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return pattern.hashCode();
	}

	@Override
	public TCType getType()
	{
		return type;
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		Value v = null;

		try
		{
			// UpdatableValues are constantized as they cannot be updated.
			v = exp.eval(ctxt).convertTo(getType(), ctxt).getConstant();
			return pattern.getNamedValues(v, ctxt);
     	}
	    catch (ValueException e)
     	{
     		abort(e);
     	}
		catch (PatternMatchException e)
		{
			abort(e, ctxt);
		}

		return null;
	}

	@Override
	public boolean isValueDefinition()
	{
		return true;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseValueDefinition(this, arg);
	}
}
