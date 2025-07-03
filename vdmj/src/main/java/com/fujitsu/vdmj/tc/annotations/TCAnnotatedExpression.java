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

package com.fujitsu.vdmj.tc.annotations;

import java.util.Collections;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.visitors.TCExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Pair;

public class TCAnnotatedExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	
	public final TCAnnotation annotation;
	public final TCExpression expression;
	
	public TCAnnotatedExpression(LexLocation location, TCAnnotation annotation, TCExpression expression)
	{
		super(location);
		this.annotation = (annotation != null) ? annotation : new TCNoAnnotation();
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return (annotation == null ? annotation + " " : "") + expression;
	}

	/**
	 * If an expression has multiple annotations, the AST is built as a chain of TCAnnotatedExpressions,
	 * each pointing to the next down the chain (see ExpressionReader.readAnnotatedExpression). But it is
	 * sensible for each tcBefore/tcAfter to be called with the base TCExpression, not the next
	 * TCAnnotatedExpression. So we calculate the list once here, and call all of the tcBefore/tcAfter
	 * methods, passing the base TCExpression.
	 */
	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		Pair<TCAnnotationList, TCExpression> pair = unpackAnnotations();
		pair.first.tcBefore(pair.second, env, scope);
		TCType type = pair.second.typeCheck(env, qualifiers, scope, constraint);
		Collections.reverse(pair.first);	// Preserve nested in/out order
		pair.first.tcAfter(pair.second, type, env, scope);
		return setType(type);
	}

	private Pair<TCAnnotationList, TCExpression> unpackAnnotations()
	{
		TCAnnotationList list = new TCAnnotationList();
		list.add(this.annotation);
		TCExpression exp = this.expression;

		while (exp instanceof TCAnnotatedExpression)
		{
			TCAnnotatedExpression aexp = (TCAnnotatedExpression)exp;
			list.add(aexp.annotation);		// In AST chain order, which is text order
			exp = aexp.expression;
		}

		return new Pair<TCAnnotationList, TCExpression>(list, exp);
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAnnotatedExpression(this, arg);
	}
}
