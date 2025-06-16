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

import com.fujitsu.vdmj.Release;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Catchpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExitException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.Value;

public class INExitStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression expression;

	public INExitStatement(LexLocation location, INExpression expression)
	{
		super(location);
		this.expression = expression;
	}

	@Override
	public String toString()
	{
		return "exit" + (expression == null ? ";" : " (" + expression + ")");
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		Value v = null;

		if (Settings.release == Release.VDM_10 && ctxt.threadState.isPure())
		{
			return abort(4167, "Cannot call exit in a pure operation", ctxt);
		}

		if (expression != null)
		{
			v = expression.eval(ctxt);
		}
		else
		{
			v = new UndefinedValue();
		}
		
		check(location, ctxt, v);

		throw new ExitException(v, location, ctxt);			// BANG!!
	}
	
	/**
	 * Check whether the interpreter has any catchpoints that we should trigger.
	 */
	private void check(LexLocation execl, Context ctxt, Value thrown)
	{
		Interpreter interpreter = Interpreter.getInstance();
		
		for (Catchpoint cp: interpreter.getCatchpoints())
		{
			if (cp.check(execl, ctxt, thrown))
			{
				break;	// Just handle one?
			}
		}
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExitStatement(this, arg);
	}
}
