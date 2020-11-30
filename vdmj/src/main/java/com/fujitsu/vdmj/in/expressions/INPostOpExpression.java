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
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.ClassContext;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueMap;

public class INPostOpExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken opname;
	public final INExpression preexpression;
	public final INExpression postexpression;
	public final INErrorCaseList errors;
	public final INStateDefinition state;

	private LexLocation errorLocation;

	public INPostOpExpression(
		TCNameToken opname, INExpression preexpression, INExpression postexpression,
		INErrorCaseList errors, INStateDefinition state)
	{
		super(postexpression.location);
		this.opname = opname;
		this.preexpression = preexpression;
		this.postexpression = postexpression;
		this.errors = errors;
		this.state = state;
	}

	@Override
	public Value eval(Context ctxt)
	{
		// No break check here, as we want to stop in the expression

		// The postcondition function arguments are the function args, the
		// result, the old/new state (if any). These all exist in ctxt.
		// We find the Sigma record and expand its contents to give additional
		// values in ctxt for each field. Ditto with Sigma~.

		try
		{
    		if (state != null)
    		{
    			RecordValue sigma = ctxt.lookup(state.name).recordValue(ctxt);

    			for (TCField field: state.fields)
    			{
    				ctxt.put(field.tagname, sigma.fieldmap.get(field.tag));
    			}

    			RecordValue oldsigma = ctxt.lookup(state.name.getOldName()).recordValue(ctxt);

    			for (TCField field: state.fields)
    			{
    				ctxt.put(field.tagname.getOldName(), oldsigma.fieldmap.get(field.tag));
    			}
    		}
    		else if (ctxt instanceof ObjectContext)
    		{
    			ObjectContext octxt = (ObjectContext)ctxt;
    			TCNameToken selfname = opname.getSelfName();
    			TCNameToken oldselfname = selfname.getOldName();

    			ObjectValue self = octxt.lookup(selfname).objectValue(ctxt);
    			ValueMap oldvalues = octxt.lookup(oldselfname).mapValue(ctxt);

    			// If the opname was defined in a superclass of "self", we have
    			// to discover the subobject to populate its state variables.

    			ObjectValue subself = findObject(opname.getModule(), self);

    			if (subself == null)
    			{
    				abort(4026, "Cannot create post_op environment", ctxt);
    			}

    			// Create an object context using the "self" passed in, rather
    			// than the self that we're being called from, assuming they
    			// are different.

    			if (subself != octxt.self)
    			{
        			ObjectContext selfctxt = new ObjectContext(
        				ctxt.location, "postcondition's object", ctxt, subself);

        			selfctxt.putAll(ctxt);	// To add "RESULT" and args.
        			ctxt = selfctxt;
    			}

    			populate(ctxt, subself.type.name.getName(), oldvalues);		// To add old "~" values
    		}
    		else if (ctxt instanceof ClassContext)
    		{
    			TCNameToken selfname = opname.getSelfName();
    			TCNameToken oldselfname = selfname.getOldName();
    			ValueMap oldvalues = ctxt.lookup(oldselfname).mapValue(ctxt);
    			populate(ctxt, opname.getModule(), oldvalues);
    		}

    		// If there are errs clauses, and there is a precondition defined, then
    		// we evaluate that as well as the postcondition.

    		boolean result =
    			(errors == null || preexpression == null || preexpression.eval(ctxt).boolValue(ctxt)) &&
    			postexpression.eval(ctxt).boolValue(ctxt);

    		errorLocation = location;

    		if (errors != null)
    		{
    			for (INErrorCase err: errors)
    			{
    				boolean left  = err.left.eval(ctxt).boolValue(ctxt);
    				boolean right = err.right.eval(ctxt).boolValue(ctxt);

    				if (left && !right)
    				{
    					errorLocation = err.left.location;
    				}

    				result = result || (left && right);
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
	public LexLocation getLocation()
	{
		return errorLocation;
	}

	private void populate(Context ctxt, String classname, ValueMap oldvalues) throws ValueException
	{
		for (Value var: oldvalues.keySet())
		{
			String name = var.stringValue(ctxt);
			Value val = oldvalues.get(var);

			if (!(val instanceof FunctionValue) &&
				!(val instanceof OperationValue))
			{
				TCNameToken oldname = new TCNameToken(location, classname, name, true, false);
				ctxt.put(oldname, val);
			}
		}
	}

	private ObjectValue findObject(String classname, ObjectValue object)
	{
		if (object.type.name.getName().equals(classname))
		{
			return object;
		}

		ObjectValue found = null;

		for (ObjectValue ov: object.superobjects)
		{
			found = findObject(classname, ov);

			if (found != null)
			{
				break;
			}
		}

		return found;
	}

	@Override
	public String toString()
	{
		return postexpression.toString();
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.casePostOpExpression(this, arg);
	}
}
