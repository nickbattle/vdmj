/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.in.annotations;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INAnnotatedExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	
	public final INAnnotation annotation;

	public final INExpression expression;
	
	public INAnnotatedExpression(LexLocation location, INAnnotation annotation, INExpression expression)
	{
		super(location);
		this.annotation = annotation;
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return annotation + " " + expression;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		annotation.before(ctxt, this);
		Value rv = expression.eval(ctxt);
		annotation.after(ctxt, this);
		return rv;
	}
	public INExpression findExpression(int lineno)
	{
		return (location.startLine == lineno) ? this : null;
	}

	public ValueList getValues(Context ctxt)
	{
		return expression.getValues(ctxt);
	}
	
	public TCNameList getOldNames()
	{
		return expression.getOldNames();
	}

	public INExpressionList getSubExpressions()
	{
		return expression.getSubExpressions();
	}
}
