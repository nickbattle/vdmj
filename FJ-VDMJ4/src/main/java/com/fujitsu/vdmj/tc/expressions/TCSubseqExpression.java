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

import com.fujitsu.vdmj.tc.lex.TCNameSet;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCSubseqExpression extends TCExpression
{
	private static final long serialVersionUID = 1L;
	public final TCExpression seq;
	public final TCExpression from;
	public final TCExpression to;
	private TCType ftype;
	private TCType ttype;

	public TCSubseqExpression(TCExpression seq, TCExpression from, TCExpression to)
	{
		super(seq);
		this.seq = seq;
		this.from = from;
		this.to = to;
	}

	@Override
	public String toString()
	{
		return "(" + seq + "(" + from + ", ... ," + to + "))";
	}

	@Override
	public TCType typeCheck(Environment env, TCTypeList qualifiers, NameScope scope, TCType constraint)
	{
		TCType stype = seq.typeCheck(env, null, scope, null);
		ftype = from.typeCheck(env, null, scope, null);
		ttype = to.typeCheck(env, null, scope, null);

		if (!stype.isSeq(location))
		{
			report(3174, "Subsequence is not of a sequence type");
		}

		if (!ftype.isNumeric(location))
		{
			report(3175, "Subsequence range start is not a number");
		}

		if (!ttype.isNumeric(location))
		{
			report(3176, "Subsequence range end is not a number");
		}

		return stype;
	}

	@Override
	public TCNameSet getFreeVariables(Environment env)
	{
		TCNameSet names = seq.getFreeVariables(env);
		names.addAll(from.getFreeVariables(env));
		names.addAll(to.getFreeVariables(env));
		return names;
	}
}
