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

import com.fujitsu.vdmj.in.definitions.INStateDefinition;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.statements.INErrorCase;
import com.fujitsu.vdmj.in.statements.INErrorCaseList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;

public class INPreOpExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final INExpression expression;
	public final INErrorCaseList errors;
	public final INStateDefinition state;

	public INPreOpExpression(
		TCNameToken opname, INExpression expression, INErrorCaseList errors, INStateDefinition state)
	{
		super(expression);
		this.opname = opname;
		this.expression = expression;
		this.errors = errors;
		this.state = state;
	}

	@Override
	public Value eval(Context ctxt)
	{
    	try
    	{
    		breakpoint.check(location, ctxt);

    		// The precondition function arguments are the function args,
    		// plus the state (if any). These all exist in ctxt. We find the
    		// Sigma record and expand its contents to give additional
    		// values in ctxt for each field.

    		if (state != null)
    		{
    			try
    			{
    				RecordValue sigma = ctxt.lookup(state.name).recordValue(ctxt);

    				for (TCField field: state.fields)
    				{
    					ctxt.put(field.tagname, sigma.fieldmap.get(field.tag));
    				}
    			}
    			catch (ValueException e)
    			{
    				abort(e);
    			}
    		}
    		else if (ctxt instanceof ObjectContext)
    		{
    			ObjectContext octxt = (ObjectContext)ctxt;
    			TCNameToken selfname = opname.getSelfName();
    			ObjectValue self = octxt.lookup(selfname).objectValue(ctxt);

    			// Create an object context using the "self" passed in, rather
    			// than the self that we're being called from.

    			ObjectContext selfctxt = new ObjectContext(
    				ctxt.location, "precondition's object", ctxt, self);

    			selfctxt.putAll(ctxt);	// To add "RESULT" and args.
    			ctxt = selfctxt;
    		}

    		boolean result = expression.eval(ctxt).boolValue(ctxt);

    		if (errors != null)
    		{
    			for (INErrorCase err: errors)
    			{
    				result = result || err.left.eval(ctxt).boolValue(ctxt);
    			}
    		}

    		return new BooleanValue(result);
    	}
    	catch (ValueException e)
    	{
    		return abort(e);
    	}
	}

	@Override
	public String toString()
	{
		return expression.toString();
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePreOpExpression(this, arg);
	}
}
