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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCIfStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression ifExp;
	public final TCStatement thenStmt;
	public final TCElseIfStatementList elseList;
	public final TCStatement elseStmt;

	public TCIfStatement(LexLocation location,
		TCExpression ifExp, TCStatement thenStmt,
		TCElseIfStatementList elseList, TCStatement elseStmt)
	{
		super(location);
		this.ifExp = ifExp;
		this.thenStmt = thenStmt;
		this.elseList = elseList;
		this.elseStmt = elseStmt;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("if " + ifExp + "\nthen\n" + thenStmt);

		for (TCElseIfStatement s: elseList)
		{
			sb.append(s.toString());
		}

		if (elseStmt != null)
		{
			sb.append("else\n");
			sb.append(elseStmt.toString());
		}

		return sb.toString();
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCType test = ifExp.typeCheck(env, null, scope, null);

		if (!test.isType(TCBooleanType.class, location))
		{
			ifExp.report(3224, "If expression is not boolean");
		}

		TCDefinitionList qualified = ifExp.getQualifiedDefs(env);
		Environment qenv = env;
		
		if (!qualified.isEmpty())
		{
			qenv = new FlatEnvironment(qualified, env);
		}

		TCTypeSet rtypes = new TCTypeSet();
		rtypes.add(thenStmt.typeCheck(qenv, scope, constraint, mandatory));

		if (elseList != null)
		{
			for (TCElseIfStatement stmt: elseList)
			{
				rtypes.add(stmt.typeCheck(env, scope, constraint, mandatory));
			}
		}

		if (elseStmt != null)
		{
			rtypes.add(elseStmt.typeCheck(env, scope, constraint, mandatory));
			return setType(rtypes.getType(location));	// check return done in elseStmt
		}
		else
		{
			rtypes.add(new TCVoidType(location));
			return checkReturnType(constraint, rtypes.getType(location), mandatory);
		}
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIfStatement(this, arg);
	}
}
