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

import java.util.Collections;

import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.util.Pair;
import com.fujitsu.vdmj.values.Value;

public class INAnnotatedStatement extends INStatement
{
	private static final long serialVersionUID = 1L;

	public final INAnnotation annotation;
	public final INStatement statement;
	
	public INAnnotatedStatement(LexLocation location, INAnnotation annotation, INStatement statement)
	{
		super(location);
		this.annotation = (annotation != null) ? annotation : new INNoAnnotation();
		this.statement = statement;
		addAnnotation(annotation);
	}

	@Override
	public String toString()
	{
		return annotation + " " + statement;
	}

	/**
	 * If a statement has multiple annotations, the AST is built as a chain of INAnnotatedStatements,
	 * each pointing to the next down the chain (see StatementReader.readStatement). But it is sensible
	 * for each tcBefore/tcAfter to be called with the base INStatement, not the next INAnnotatedStatement.
	 * So we calculate the list once here, and call all of the tcBefore/tcAfter methods, passing the
	 * base INStatement. The base statement's typeCheck is only called once.
	 */
	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Pair<INAnnotationList, INStatement> pair = unpackAnnotations();
		if (!INAnnotation.suspended) pair.first.inBefore(pair.second, ctxt);
		Value rv = pair.second.eval(ctxt);
		Collections.reverse(pair.first);	// Preserve nested in/out order
		if (!INAnnotation.suspended) pair.first.inAfter(pair.second, rv, ctxt);
		return rv;
	}
	
	private Pair<INAnnotationList, INStatement> unpackAnnotations()
	{
		INAnnotationList list = new INAnnotationList();
		list.add(this.annotation);
		INStatement stmt = this.statement;

		while (stmt instanceof INAnnotatedStatement)
		{
			INAnnotatedStatement astmt = (INAnnotatedStatement)stmt;
			list.add(astmt.annotation);		// In AST chain order, which is text order
			stmt = astmt.statement;
		}

		return new Pair<INAnnotationList, INStatement>(list, stmt);
	}

	@Override
	public void addAnnotation(INAnnotation annotation)
	{
		statement.addAnnotation(annotation);
	}
	
	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAnnotatedStatement(this, arg);
	}
}
