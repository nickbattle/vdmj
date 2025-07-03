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
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Pair;

public class TCAnnotatedStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;

	public final TCAnnotation annotation;
	public final TCStatement statement;
	
	public TCAnnotatedStatement(LexLocation location, TCAnnotation annotation, TCStatement statement)
	{
		super(location);
		this.annotation = (annotation != null) ? annotation : new TCNoAnnotation();
		this.statement = statement;
		setType(statement.getType());

		TCStatement stmt = this.statement;

		while (stmt instanceof TCAnnotatedStatement)
		{
			TCAnnotatedStatement astmt = (TCAnnotatedStatement)stmt;
			stmt = astmt.statement;
		}

		stmt.addAnnotation(annotation);
	}

	@Override
	public String toString()
	{
		return annotation + " " + statement;
	}

	/**
	 * If a statement has multiple annotations, the AST is built as a chain of TCAnnotatedStatements,
	 * each pointing to the next down the chain (see SyntaxReader.readStatement). But it is sensible
	 * for each tcBefore/tcAfter to be called with the base TCStatement, not the next TCAnnotatedStatement.
	 * So we calculate the list once here, and call all of the tcBefore/tcAfter methods, passing the
	 * base TCStatement.
	 */
	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		Pair<TCAnnotationList, TCStatement> pair = unpackAnnotations();
		pair.first.tcBefore(pair.second, env, scope);
		TCType type = pair.second.typeCheck(env, scope, constraint, mandatory);
		Collections.reverse(pair.first);	// Preserve nested in/out order
		pair.first.tcAfter(pair.second, type, env, scope);
		return setType(type);
	}

	private Pair<TCAnnotationList, TCStatement> unpackAnnotations()
	{
		TCAnnotationList list = new TCAnnotationList();
		list.add(this.annotation);
		TCStatement stmt = this.statement;

		while (stmt instanceof TCAnnotatedStatement)
		{
			TCAnnotatedStatement astmt = (TCAnnotatedStatement)stmt;
			list.add(astmt.annotation);		// In AST chain order, which is text order
			stmt = astmt.statement;
		}

		return new Pair<TCAnnotationList, TCStatement>(list, stmt);
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAnnotatedStatement(this, arg);
	}
}
