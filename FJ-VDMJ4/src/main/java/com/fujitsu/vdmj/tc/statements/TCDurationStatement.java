/*******************************************************************************
 *
 *	Copyright (c) 2008, 2016 Fujitsu Services Ltd.
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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCDurationStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression duration;
	public final TCStatement statement;

	public TCDurationStatement(
		LexLocation location, TCExpression duration, TCStatement stmt)
	{
		super(location);
		this.duration = duration;
		this.statement = stmt;
	}

	@Override
	public String toString()
	{
		return "duration (" + duration + ") " + statement;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCDefinition encl = env.getEnclosingDefinition();
		
		if (encl != null && encl.isPure())
		{
			report(3346, "Cannot use duration in pure operations");
		}
		
		Environment functional = new FlatEnvironment(env, true);
		TCType argType = duration.typeCheck(functional, null, scope, null);
		
		if (!TypeComparator.compatible(new TCNaturalType(location), argType))
		{
			duration.report(3281, "Arguments to duration must be a nat");
			detail("Actual", argType);
		}

		return statement.typeCheck(env, scope, constraint, mandatory);
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		TCNameSet names = duration.getFreeVariables(globals, env);
		names.addAll(statement.getFreeVariables(globals, env, returns));
		return names;
	}
}
