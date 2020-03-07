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
import com.fujitsu.vdmj.tc.expressions.TCExpression;
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
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		expType = exp.typeCheck(env, null, scope, null);
		TCTypeSet rtypes = new TCTypeSet();
		boolean always = false;

		for (TCCaseStmtAlternative c: cases)
		{
			rtypes.add(c.typeCheck(env, scope, expType, constraint, mandatory));
			always = always || c.pattern.alwaysMatches(expType);
		}

		if (others != null)
		{
			rtypes.add(others.typeCheck(env, scope, constraint, mandatory));
		}
		else if (!always)
		{
			rtypes.add(new TCVoidType(location));
		}

		return rtypes.getType(location);
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesStatement(this, arg);
	}
}
