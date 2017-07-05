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

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCExitStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression expression;
	private TCType exptype = null;

	public TCExitStatement(LexLocation location, TCExpression expression)
	{
		super(location);
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "exit" + (expression == null ? ";" : " (" + expression + ")");
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint)
	{
		if (expression != null)
		{
			exptype = expression.typeCheck(env, null, scope, null);
		}

		// This is unknown because the statement doesn't actually return a
		// value - so if this is the only statement in a body, it is not a
		// type error (should return the same type as the definition return
		// type).

		return new TCUnknownType(location);
	}

	@Override
	public TCTypeSet exitCheck()
	{
		TCTypeSet types = new TCTypeSet();

		if (expression == null)
		{
			types.add(new TCVoidType(location));
		}
		else
		{
			types.add(exptype);
		}

		return types;
	}

	@Override
	public TCNameSet getFreeVariables(Environment env, AtomicBoolean returns)
	{
		returns.set(true);

		if (expression == null)
		{
			return new TCNameSet();
		}
		
		return expression.getFreeVariables(env);
	}
}
