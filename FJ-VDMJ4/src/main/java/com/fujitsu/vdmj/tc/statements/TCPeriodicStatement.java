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

import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCExplicitOperationDefinition;
import com.fujitsu.vdmj.tc.definitions.TCImplicitOperationDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCPeriodicStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final TCExpressionList args;

	public TCPeriodicStatement(TCNameToken opname, TCExpressionList args)
	{
		super(opname.getLocation());
		this.opname = opname;
		this.args = args;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		if (args.size() != 4)
		{
			report(3287, "Periodic thread must have 4 argument(s)");
		}
		else
		{
			Environment functional = new FlatEnvironment(env, true);

			for (TCExpression arg: args)
			{
				TCType type = arg.typeCheck(functional, null, scope, null);
				
				if (!type.isNumeric(location))
				{
					arg.report(3316, "Expecting number in periodic argument");
				}
			}
		}

		opname.setTypeQualifier(new TCTypeList());
		TCDefinition opdef = env.findName(opname, NameScope.NAMES);

		if (opdef == null)
		{
			report(3228, opname + " is not in scope");
			return new TCUnknownType(location);
		}

		// Operation must be "() ==> ()"

		TCOperationType expected =
			new TCOperationType(location, new TCTypeList(), new TCVoidType(location));
		
		opdef = opdef.deref();

		if (opdef instanceof TCExplicitOperationDefinition)
		{
			TCExplicitOperationDefinition def = (TCExplicitOperationDefinition)opdef;

			if (!def.type.equals(expected))
			{
				report(3229, opname + " should have no parameters or return type");
				detail("Actual", def.type);
			}
			else if (def.isPure())
			{
				report(3347, "Cannot have a pure operation as the body of a thread");
			}
		}
		else if (opdef instanceof TCImplicitOperationDefinition)
		{
			TCImplicitOperationDefinition def = (TCImplicitOperationDefinition)opdef;

			if (def.body == null)
			{
				report(3230, opname + " is implicit");
			}

			if (!def.type.equals(expected))
			{
				report(3231, opname + " should have no parameters or return type");
				detail("Actual", def.type);
			}
			else if (def.isPure())
			{
				report(3347, "Cannot have a pure operation as the body of a thread");
			}
		}
		else
		{
			report(3232, opname + " is not an operation name");
		}

		return new TCVoidType(location);
	}

	@Override
	public String toString()
	{
		return "periodic(" + Utils.listToString(args) + ")(" + opname + ")";
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		return args.getFreeVariables(globals, env);
	}
}
