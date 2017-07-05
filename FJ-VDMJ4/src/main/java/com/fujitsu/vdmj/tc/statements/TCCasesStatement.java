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
import com.fujitsu.vdmj.tc.types.TCVoidType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCCasesStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCExpression exp;
	public final TCCaseStmtAlternativeList cases;
	public final TCStatement others;

	public TCType expType = null;

	public TCCasesStatement(LexLocation location,
		TCExpression exp, TCCaseStmtAlternativeList cases, TCStatement others)
	{
		super(location);
		this.exp = exp;
		this.cases = cases;
		this.others = others;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("cases " + exp + " :\n");

		for (TCCaseStmtAlternative csa: cases)
		{
			sb.append("  ");
			sb.append(csa.toString());
		}

		if (others != null)
		{
			sb.append("  others -> ");
			sb.append(others.toString());
		}

		sb.append("esac");
		return sb.toString();
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint)
	{
		expType = exp.typeCheck(env, null, scope, null);
		TCTypeSet rtypes = new TCTypeSet();

		for (TCCaseStmtAlternative c: cases)
		{
			rtypes.add(c.typeCheck(env, scope, expType, constraint));
		}

		if (others != null)
		{
			rtypes.add(others.typeCheck(env, scope, constraint));
		}
		else
		{
			rtypes.add(new TCVoidType(location));
		}

		return rtypes.getType(location);
	}


	@Override
	public TCTypeSet exitCheck()
	{
		TCTypeSet types = new TCTypeSet();

		for (TCCaseStmtAlternative c: cases)
		{
			types.addAll(c.exitCheck());
		}

		return types;
	}

	@Override
	public TCNameSet getFreeVariables(Environment env, AtomicBoolean returns)
	{
		return exp.getFreeVariables(env);	// Cases are conditional
	}
}
