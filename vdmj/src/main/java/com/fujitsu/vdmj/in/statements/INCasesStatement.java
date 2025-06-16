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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

public class INCasesStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression exp;
	public final INCaseStmtAlternativeList cases;
	public final INStatement others;

	public INCasesStatement(LexLocation location,
		INExpression exp, INCaseStmtAlternativeList cases, INStatement others)
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

		for (INCaseStmtAlternative csa: cases)
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
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value val = exp.eval(ctxt);

		for (INCaseStmtAlternative c: cases)
		{
			Value rv = c.eval(val, ctxt);
			if (rv != null) return rv;
		}

		if (others != null)
		{
			return others.eval(ctxt);
		}

		return new VoidValue();
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesStatement(this, arg);
	}
}
