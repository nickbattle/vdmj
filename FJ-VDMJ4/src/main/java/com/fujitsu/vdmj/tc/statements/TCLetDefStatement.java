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
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCExplicitFunctionDefinition;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCLetDefStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCDefinitionList localDefs;
	public final TCStatement statement;

	public TCLetDefStatement(LexLocation location,
		TCDefinitionList localDefs, TCStatement statement)
	{
		super(location);
		this.localDefs = localDefs;
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return "let " + localDefs + " in " + statement;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint)
	{
		// Each local definition is in scope for later local definitions...

		Environment local = env;

		for (TCDefinition d: localDefs)
		{
			if (d instanceof TCExplicitFunctionDefinition)
			{
				// Functions' names are in scope in their bodies, whereas
				// simple variable declarations aren't

				local = new FlatCheckedEnvironment(d, local, scope);	// cumulative
				d.implicitDefinitions(local);
				d.typeResolve(local);

				if (env.isVDMPP())
				{
					TCClassDefinition cdef = env.findClassDefinition();
					d.setClassDefinition(cdef);
					d.setAccessSpecifier(d.accessSpecifier.getStatic(true));
				}

				d.typeCheck(local, scope);
			}
			else
			{
				d.implicitDefinitions(local);
				d.typeResolve(local);
				d.typeCheck(local, scope);
				local = new FlatCheckedEnvironment(d, local, scope);	// cumulative
			}
		}

		TCType r = statement.typeCheck(local, scope, constraint);
		local.unusedCheck(env);
		return r;
	}

	@Override
	public TCTypeSet exitCheck()
	{
		return statement.exitCheck();
	}

	@Override
	public TCNameSet getFreeVariables(Environment env, AtomicBoolean returns)
	{
		Environment local = env;
		TCNameSet names = new TCNameSet();

		for (TCDefinition d: localDefs)
		{
			if (d instanceof TCExplicitFunctionDefinition)
			{
				// ignore
			}
			else
			{
				local = new FlatEnvironment(d, local);
				names.addAll(d.getFreeVariables(local, returns));
			}
		}

		names.addAll(statement.getFreeVariables(local, returns));
		return names;
	}
}
