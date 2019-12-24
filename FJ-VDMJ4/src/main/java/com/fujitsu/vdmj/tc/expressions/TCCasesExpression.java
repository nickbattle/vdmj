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

package com.fujitsu.vdmj.tc.expressions;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.util.Utils;

public class TCCasesExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression exp;
	public final TCCaseAlternativeList cases;
	public final TCExpression others;
	public TCType expType = null;

	public TCCasesExpression(LexLocation location, TCExpression exp,
					TCCaseAlternativeList cases, TCExpression others)
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
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		expType = exp.typeCheck(env, null, scope, null);
		TCTypeSet rtypes = new TCTypeSet();

		for (TCCaseAlternative c: cases)
		{
			rtypes.add(c.typeCheck(env, scope, expType, constraint));
		}

		if (others != null)
		{
			rtypes.add(others.typeCheck(env, null, scope, constraint));
		}

		return rtypes.getType(location);
	}

	@Override
	public TCNameSet getFreeVariables(Environment globals, Environment env)
	{
		return exp.getFreeVariables(globals, env);	// The rest is conditional
	}

	@Override
	public <R, S> R apply(TCExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCasesExpression(this, arg);
	}
}
