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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

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
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		found = exp.findExpression(lineno);
		if (found != null) return found;

		for (INCaseAlternative c: cases)
		{
			found = c.result.findExpression(lineno);
			if (found != null) break;
		}

		return found != null ? found :
				others != null ? others.findExpression(lineno) : null;
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
	public ValueList getValues(Context ctxt)
	{
		ValueList list = exp.getValues(ctxt);

		for (INCaseAlternative c: cases)
		{
			list.addAll(c.getValues(ctxt));
		}

		if (others != null)
		{
			list.addAll(others.getValues(ctxt));
		}

		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = exp.getOldNames();

		for (INCaseAlternative c: cases)
		{
			list.addAll(c.getOldNames());
		}

		if (others != null)
		{
			list.addAll(others.getOldNames());
		}

		return list;
	}

	@Override
	public INExpressionList getSubExpressions()
	{
		INExpressionList subs = exp.getSubExpressions();

		for (INCaseAlternative c: cases)
		{
			subs.addAll(c.getSubExpressions());
		}

		if (others != null)
		{
			subs.addAll(others.getSubExpressions());
		}

		subs.add(this);
		return subs;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesExpression(this, arg);
	}
}
