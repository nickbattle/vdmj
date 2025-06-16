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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCNonDeterministicStatement extends TCSimpleBlockStatement
{
	private static final long serialVersionUID = 1L;

	public TCNonDeterministicStatement(LexLocation location, TCStatementList statements)
	{
		super(location, statements);
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCDefinition encl = env.getEnclosingDefinition();
		
		if (encl != null && encl.isPure())
		{
			report(3346, "Cannot use non-deterministic statement in pure operations");
		}

		TCTypeSet rtypes = new TCTypeSet();
		int rcount = 0;

		for (TCStatement stmt: statements)
		{
			TCType stype = stmt.typeCheck(env, scope, constraint, mandatory);

			if (stype instanceof TCUnionType)
			{
				TCUnionType ust = (TCUnionType)stype;

				for (TCType t: ust.types)
				{
					if (addOne(rtypes, t)) rcount++;
				}
			}
			else
			{
				if (addOne(rtypes, stype)) rcount++;
			}
		}
		
		if (rcount > 1)
		{
			warning(5016, "Some statements will not be reached");
		}

		return setType(rtypes.isEmpty() ?
			new TCVoidType(location) : rtypes.getType(location));
	}

	private boolean addOne(TCTypeSet rtypes, TCType add)
	{
		if (add instanceof TCVoidReturnType)
		{
			rtypes.add(new TCVoidType(add.location));
			return true;
		}
		else if (!(add instanceof TCVoidType))
		{
			rtypes.add(add);
			return true;
		}
		else
		{
			rtypes.add(add);
			return false;
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("||(\n");
		sb.append(super.toString());
		sb.append(")");
		return sb.toString();
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNonDeterministicStatement(this, arg);
	}
}
