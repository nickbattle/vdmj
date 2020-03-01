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
import com.fujitsu.vdmj.tc.definitions.TCAssignmentDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCBlockStatement extends TCSimpleBlockStatement
{
	private static final long serialVersionUID = 1L;

	public final TCDefinitionList assignmentDefs;

	public TCBlockStatement(LexLocation location, TCDefinitionList assignmentDefs, TCStatementList statements)
	{
		super(location, statements);
		this.assignmentDefs = assignmentDefs;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		// Each dcl definition is in scope for later definitions...

		Environment local = env;

		for (TCDefinition d: assignmentDefs)
		{
			local = new FlatCheckedEnvironment(d, local, scope);	// cumulative
			d.implicitDefinitions(local);
			d.typeCheck(local, scope);
		}

		// For type checking purposes, the definitions are treated as
		// local variables. At runtime (below) they have to be treated
		// more like (updatable) state.

		TCType r = super.typeCheck(local, scope, constraint, mandatory);
		local.unusedCheck(env);
		return r;
	}

	@Override
	public TCTypeSet exitCheck(Environment base)
	{
		TCTypeSet types = super.exitCheck(base);
		
		for (TCDefinition d: assignmentDefs)
		{
			if (d instanceof TCAssignmentDefinition)
			{
				TCAssignmentDefinition ad = (TCAssignmentDefinition)d;
				types.addAll(ad.expression.exitCheck(base));
			}
		}
		
		return types;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(\n");

		for (TCDefinition d: assignmentDefs)
		{
			sb.append(d);
			sb.append("\n");
		}

		sb.append("\n");
		sb.append(super.toString());
		sb.append(")");
		return sb.toString();
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env, AtomicBoolean returns)
	{
		Environment local = env;

		for (TCDefinition d: assignmentDefs)
		{
			local = new FlatEnvironment(d, local);	// cumulative
		}

		return super.getFreeVariables(globals, local, returns);
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBlockStatement(this, arg);
	}
}
