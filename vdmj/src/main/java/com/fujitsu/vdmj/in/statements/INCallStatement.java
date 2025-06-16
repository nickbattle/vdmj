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
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INCallStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	public final INExpressionList args;

	public INCallStatement(TCNameToken name, INExpressionList args)
	{
		super(name.getLocation());
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return name.getName() + "(" + Utils.listToString(args) + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		boolean endstop = breakpoint.catchReturn(ctxt);

		try
		{
			Value v = ctxt.lookup(name).deref();

			if (v instanceof OperationValue)
			{
    			OperationValue op = v.operationValue(ctxt);
    			ValueList argValues = new ValueList();

    			for (INExpression arg: args)
    			{
    				argValues.add(arg.eval(ctxt));
    			}

    			Value rv = op.eval(location, argValues, ctxt);

    			if (endstop && !breakpoint.isContinue(ctxt))
           		{
           			ctxt.addResult(location, this.toString(), rv);
           			breakpoint.enterDebugger(ctxt);
           			ctxt.removeResult(location);
           		}
           		
    			return rv;
			}
			else
			{
    			FunctionValue fn = v.functionValue(ctxt);
    			ValueList argValues = new ValueList();

    			for (INExpression arg: args)
    			{
    				argValues.add(arg.eval(ctxt));
    			}

    			Value rv = fn.eval(location, argValues, ctxt);

    			if (endstop && !breakpoint.isContinue(ctxt))
           		{
           			ctxt.addResult(location, this.toString(), rv);
           			breakpoint.enterDebugger(ctxt);
           			ctxt.removeResult(location);
           		}
           		
    			return rv;
			}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseCallStatement(this, arg);
	}
}
