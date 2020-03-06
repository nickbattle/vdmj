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
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCInstanceVariableDefinition;
import com.fujitsu.vdmj.tc.definitions.TCStateDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeComparator;

public class TCAssignmentStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;

	public final TCExpression exp;
	public final TCStateDesignator target;
	public TCType targetType;
	public TCType expType;
	public TCClassDefinition classDefinition = null;
	public TCStateDefinition stateDefinition = null;
	public boolean inConstructor = false;

	public TCAssignmentStatement(LexLocation location, TCStateDesignator target, TCExpression exp)
	{
		super(location);
		this.exp = exp;
		this.target = target;
	}

	@Override
	public String toString()
	{
		return target + " := " + exp;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		targetType = target.typeCheck(env);
		expType = exp.typeCheck(env, null, scope, targetType);

		if (!TypeComparator.compatible(targetType, expType))
		{
			report(3239, "Incompatible types in assignment");
			detail2("Target", targetType, "Expression", expType);
		}

		classDefinition = env.findClassDefinition();
		stateDefinition = env.findStateDefinition();
		inConstructor = inConstructor(env);

		if (inConstructor)
		{
			// Mark assignment target as initialized (so no warnings)

			TCDefinition state = target.targetDefinition(env);

			if (state instanceof TCInstanceVariableDefinition)
			{
				TCInstanceVariableDefinition iv = (TCInstanceVariableDefinition)state;
				iv.initialized = true;
			}
		}

		return checkReturnType(constraint, new TCVoidType(location), mandatory);
	}

	@Override
	public TCTypeSet exitCheck(Environment base)
	{
		return exp.exitCheck(base);
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAssignmentStatement(this, arg);
	}
}
