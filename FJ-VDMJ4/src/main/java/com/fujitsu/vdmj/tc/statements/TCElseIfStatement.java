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

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCElseIfStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression elseIfExp;
	public final TCStatement thenStmt;

	public TCElseIfStatement(LexLocation location, TCExpression elseIfExp, TCStatement thenStmt)
	{
		super(location);
		this.elseIfExp = elseIfExp;
		this.thenStmt = thenStmt;
	}

	@Override
	public String toString()
	{
		return "elseif " + elseIfExp + "\nthen\n" + thenStmt;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		if (!elseIfExp.typeCheck(env, null, scope, null).isType(TCBooleanType.class, location))
		{
			elseIfExp.report(3218, "Expression is not boolean");
		}

		TCDefinitionList qualified = elseIfExp.getQualifiedDefs(env);
		Environment qenv = env;
		
		if (!qualified.isEmpty())
		{
			qenv = new FlatEnvironment(qualified, env);
		}

		return thenStmt.typeCheck(qenv, scope, constraint, mandatory);
	}

	@Override
	public TCTypeSet exitCheck(Environment base)
	{
		TCTypeSet result = elseIfExp.exitCheck(base);
		result.addAll(thenStmt.exitCheck(base));
		return result;
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseElseIfStatement(this, arg);
	}
}
