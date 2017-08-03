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

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.util.Utils;
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

	public INApplyExpression(INExpression root)
	{
		super(root);
		this.root = root;
		this.args = new INExpressionList();	// ie. "()"
	}

	public INApplyExpression(INExpression root, INExpressionList args)
	{
		super(root);
		this.root = root;
		this.args = args;
	}

	@Override
	public String toString()
	{
		return root + "("+ Utils.listToString(args) + ")";
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		found = root.findExpression(lineno);
		if (found != null) return found;

		return args.findExpression(lineno);
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
           		
           		if (endstop)	// Catch after the return if we didn't skip
           		{
           			breakpoint.enterDebugger(ctxt);
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
           		
           		if (endstop)	// Catch after the return if we didn't skip
           		{
           			breakpoint.enterDebugger(ctxt);
           		}
           		
           		return rv;
    		}
			else if (object instanceof SeqValue)
    		{
    			Value arg = args.get(0).eval(ctxt);
    			SeqValue sv = (SeqValue)object;
    			return sv.get(arg, ctxt);
    		}
			else if (object instanceof MapValue)
    		{
    			Value arg = args.get(0).eval(ctxt);
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
	public ValueList getValues(Context ctxt)
	{
		ValueList list = args.getValues(ctxt);
		list.addAll(root.getValues(ctxt));
		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = args.getOldNames();
		list.addAll(root.getOldNames());
		return list;
	}

	@Override
	public INExpressionList getSubExpressions()
	{
		INExpressionList subs = args.getSubExpressions();
		subs.addAll(root.getSubExpressions());
		subs.add(this);
		return subs;
	}
}
