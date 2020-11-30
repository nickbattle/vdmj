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

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.Value;

public class INCasesExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression exp;
	public final INCaseAlternativeList cases;
	public final INExpression others;

	public INCasesExpression(LexLocation location, INExpression exp,
					INCaseAlternativeList cases, INExpression others)
	{
		super(location);
		this.exp = exp;
		this.cases = cases;
		this.others = others;
	}

	@Override
	public String toString()
	{
		return "(cases " + exp + " :\n" +
			Utils.listToString("", cases, ",\n", "") +
			(others == null ? "\n" : "\nothers -> " + others + "\n") + "end)";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value val = exp.eval(ctxt);

		for (INCaseAlternative c: cases)
		{
			Value rv = c.eval(val, ctxt);
			if (rv != null) return rv;
		}

		if (others != null)
		{
			return others.eval(ctxt);
		}

		return abort(4004, "No cases apply for " + val, ctxt);
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesExpression(this, arg);
	}
}
