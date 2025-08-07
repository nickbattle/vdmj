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
import com.fujitsu.vdmj.tc.annotations.TCLoopAnnotations;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.expressions.TCBooleanLiteralExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCWhileStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression exp;
	public final TCStatement statement;

	private TCLoopAnnotations invariants = null;

	public TCWhileStatement(LexLocation location, TCExpression exp, TCStatement body)
	{
		super(location);
		this.exp = exp;
		this.statement = body;
	}

	@Override
	public String toString()
	{
		return "while " + exp + " do " + statement;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		if (!exp.typeCheck(env, null, scope, null).isType(TCBooleanType.class, location))
		{
			exp.report(3218, "Expression is not boolean");
		}

		TCDefinitionList qualified = exp.getQualifiedDefs(env);
		Environment qenv = env;
		
		if (!qualified.isEmpty())
		{
			qenv = new FlatEnvironment(qualified, env);
		}
		
		if (exp instanceof TCBooleanLiteralExpression)
		{
			mandatory = false;	// while true and while false effectively suppress constraints
		}

		TCType stype = statement.typeCheck(qenv, scope, constraint, mandatory);
		
		if (exp instanceof TCBooleanLiteralExpression)
		{
			if (stype instanceof TCUnionType)
			{
				TCBooleanLiteralExpression ble = (TCBooleanLiteralExpression)exp;
				
				if (ble.value.value)	// while true do...
				{
					TCTypeSet edited = new TCTypeSet();
					TCUnionType original = (TCUnionType)stype;
					
					for (TCType t: original.types)
					{
						if (!(t instanceof TCVoidType))
						{
							edited.add(t);
						}
					}
					
					stype = new TCUnionType(stype.location, edited);
				}
			}
		}
		else
		{
			// The while condition may never be true, so all while loops can also return void.
			// BUT this catches too many cases that are actually safe, so we don't add ()...

			// stype = new TCUnionType(location, stype, new TCVoidType(location));
		}

		invariants = TCLoopAnnotations.getLoopAnnotations(this);
		invariants.typeCheck(env, this);
		
		return setType(stype);
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseWhileStatement(this, arg);
	}
}
