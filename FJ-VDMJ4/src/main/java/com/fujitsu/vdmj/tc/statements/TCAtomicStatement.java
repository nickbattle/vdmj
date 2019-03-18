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
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCAtomicStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;

	public final TCAssignmentStatementList assignments;

	public TCAtomicStatement(LexLocation location, TCAssignmentStatementList assignments)
	{
		super(location);
		this.assignments = assignments;
	}

	@Override
	public String toString()
	{
		return "atomic (" + Utils.listToString(assignments) + ")";
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		for (TCAssignmentStatement stmt: assignments)
		{
			stmt.typeCheck(env, scope, constraint, mandatory);
		}

		return new TCVoidType(location);
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		TCNameSet names = new TCNameSet();
		
		for (TCAssignmentStatement stmt: assignments)
		{
			names.addAll(stmt.getFreeVariables(globals, env, returns));
		}
		
		return names;
	}
}
