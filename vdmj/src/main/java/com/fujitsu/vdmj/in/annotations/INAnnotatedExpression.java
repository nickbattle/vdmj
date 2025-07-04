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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.annotations;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.util.Pair;
import com.fujitsu.vdmj.values.Value;

public class INAnnotatedExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	
	public final INAnnotation annotation;
	public final INExpression expression;
	
	public INAnnotatedExpression(LexLocation location, INAnnotation annotation, INExpression expression)
	{
		super(location);
		this.annotation = (annotation != null) ? annotation : new INNoAnnotation();
		this.expression = expression;
		addAnnotation(annotation);
	}

	@Override
	public String toString()
	{
		return annotation + " " + expression;
	}

	/**
	 * If a statement has multiple annotations, the AST is built as a chain of INAnnotatedStatements,
	 * each pointing to the next down the chain (see StatementReader.readStatement). But it is sensible
	 * for each tcBefore/tcAfter to be called with the base INStatement, not the next INAnnotatedStatement.
	 * So we calculate the list once here, and call all of the tcBefore/tcAfter methods, passing the
	 * base INStatement. The base expression's typeCheck is only called once.
	 */
	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Pair<INAnnotationList, INExpression> pair = unpackAnnotations();
		if (!INAnnotation.suspended) pair.first.inBefore(pair.second, ctxt);
		Value rv = pair.second.eval(ctxt);
		if (!INAnnotation.suspended) pair.first.inAfter(pair.second, rv, ctxt);
		return rv;
	}
	
	private Pair<INAnnotationList, INExpression> unpackAnnotations()
	{
		INAnnotationList list = new INAnnotationList();
		list.add(this.annotation);
		INExpression exp = this.expression;

		while (exp instanceof INAnnotatedExpression)
		{
			INAnnotatedExpression aexp = (INAnnotatedExpression)exp;
			list.add(aexp.annotation);		// In AST chain order, which is text order
			exp = aexp.expression;
		}

		return new Pair<INAnnotationList, INExpression>(list, exp);
	}

	@Override
	public void addAnnotation(INAnnotation annotation)
	{
		expression.addAnnotation(annotation);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAnnotatedExpression(this, arg);
	}
}
