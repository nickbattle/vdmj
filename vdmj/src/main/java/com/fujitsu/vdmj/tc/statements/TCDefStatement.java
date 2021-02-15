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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.statements;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCDefStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCDefinitionList equalsDefs;
	public final TCStatement statement;

	public TCDefStatement(LexLocation location, TCDefinitionList equalsDefs, TCStatement statement)
	{
		super(location);
		this.equalsDefs = equalsDefs;
		this.statement = statement;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		// Each local definition is in scope for later local definitions...

		Environment local = env;

		for (TCDefinition d: equalsDefs)
		{
			d.implicitDefinitions(local);
			d.typeResolve(local);
			d.typeCheck(local, scope);
			local = new FlatCheckedEnvironment(d, local, scope);	// cumulative
		}

		TCType r = statement.typeCheck(local, scope, constraint, mandatory);
		local.unusedCheck(env);
		return r;
	}

	@Override
	public String toString()
	{
		return "def " + Utils.listToString(equalsDefs) + " in " + statement;
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseDefStatement(this, arg);
	}
}
