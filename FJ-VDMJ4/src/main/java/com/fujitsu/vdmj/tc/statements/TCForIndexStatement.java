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
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCForIndexStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken var;
	public final TCExpression from;
	public final TCExpression to;
	public final TCExpression by;
	public final TCStatement statement;

	public TCForIndexStatement(LexLocation location,
		TCNameToken var, TCExpression from, TCExpression to, TCExpression by, TCStatement body)
	{
		super(location);
		this.var = var;
		this.from = from;
		this.to = to;
		this.by = by;
		this.statement = body;
	}

	@Override
	public String toString()
	{
		return "for " + var + " = " + from + " to " + to +
					(by == null ? "" : " by " + by) + "\n" + statement;
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		TCType ft = from.typeCheck(env, null, scope, null);
		TCType tt = to.typeCheck(env, null, scope, null);

		if (!ft.isNumeric(location))
		{
			report(3220, "From type is not numeric");
		}

		if (!tt.isNumeric(location))
		{
			report(3221, "To type is not numeric");
		}

		if (by != null)
		{
			TCType bt = by.typeCheck(env, null, scope, null);

			if (!bt.isNumeric(location))
			{
				report(3222, "By type is not numeric");
			}
		}

		TCDefinition vardef = new TCLocalDefinition(var.getLocation(), var, ft);
		Environment local = new FlatCheckedEnvironment(vardef, env, scope);
		TCType rt = statement.typeCheck(local, scope, constraint, mandatory);
		local.unusedCheck();
		return rt;
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseForIndexStatement(this, arg);
	}
}
