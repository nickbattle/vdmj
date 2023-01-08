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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INApplyExpression extends INExpression
{
	private static final long serialVersionUID = 1L;

	public final INExpression root;
	public final INExpressionList args;
	public final TCType type;

	public INApplyExpression(INExpression root, INExpressionList args, TCType type)
	{
		super(root);
		this.root = root;
		this.args = args;
		this.type = type;
	}

	@Override
	public String toString()
	{
		if (root instanceof INVariableExpression)
		{
			INVariableExpression ve = (INVariableExpression)root;
			return ve.name.getName() + "(" + argsString() + ")";
		}
		else
		{
			return root + "(" + argsString() + ")";
		}
	}
	
	private String argsString()
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";
		
		for (INExpression arg: args)
		{
			String a = arg.toString();
			
			if (a.startsWith("(") && a.endsWith(")"))
			{
				a = a.substring(1, a.length() - 1);		// eg. "(x + y)" is "x + y"
			}
			
			sb.append(sep);
			sb.append(a);
			sep = ", ";
		}
		
		return sb.toString();
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		location.hits--;	// This is counted below when root is evaluated
		boolean endstop = breakpoint.catchReturn(ctxt);

    	try
    	{
    		Value object = root.eval(ctxt).deref();

			if (object instanceof FunctionValue)
    		{
        		ValueList argvals = new ValueList();

         		for (INExpression arg: args)
        		{
        			argvals.add(arg.eval(ctxt));
        		}

           		FunctionValue fv = object.functionValue(ctxt);
           		Value rv = fv.eval(location, argvals, ctxt);
           		
           		if (endstop && !breakpoint.isContinue(ctxt))
           		{
           			ctxt.addResult(location, this.toString(), rv);
           			breakpoint.enterDebugger(ctxt);
           			ctxt.removeResult(location);
           		}
           		
           		return rv;
    		}
			else if (object instanceof OperationValue)
    		{
        		ValueList argvals = new ValueList();

         		for (INExpression arg: args)
        		{
        			argvals.add(arg.eval(ctxt));
        		}

         		OperationValue ov = object.operationValue(ctxt);
           		Value rv = ov.eval(location, argvals, ctxt);
           		
           		if (endstop && !breakpoint.isContinue(ctxt))
           		{
           			ctxt.addResult(location, this.toString(), rv);
           			breakpoint.enterDebugger(ctxt);
           			ctxt.removeResult(location);
           		}
           		
           		return rv;
    		}
			else if (object instanceof SeqValue)
    		{
    			Value arg = args.get(0).eval(ctxt).convertTo(new TCNaturalOneType(location), ctxt);
    			SeqValue sv = (SeqValue)object;
    			return sv.get(arg, ctxt);
    		}
			else if (object instanceof MapValue)
    		{
				TCMapType mtype = type.getMap();
    			Value arg = args.get(0).eval(ctxt).convertTo(mtype.from, ctxt);
    			MapValue mv = (MapValue)object;
    			return mv.lookup(arg, ctxt);
    		}
			else
			{
    			return abort(4003, "Value " + object + " cannot be applied", ctxt);
			}
    	}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseApplyExpression(this, arg);
	}
}
