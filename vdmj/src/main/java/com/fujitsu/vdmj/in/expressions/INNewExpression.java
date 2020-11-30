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

import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.util.Utils;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INNewExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final TCIdentifierToken classname;
	public final INExpressionList args;
	public final INClassDefinition classdef;	// Recursive update from mapper
	public final INDefinition ctordef;

	public INNewExpression(LexLocation location, TCIdentifierToken classname, INExpressionList args,
		INClassDefinition classdef, INDefinition ctordef)
	{
		super(location);
		this.classname = classname;
		this.args = args;
		this.classdef = classdef;
		this.ctordef = ctordef;
		
		this.classname.getLocation().executable(true);
	}

	@Override
	public String toString()
	{
		return "new " + classname + "("+ Utils.listToString(args) + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		classname.getLocation().hit();

		try
		{
    		ValueList argvals = new ValueList();

     		for (INExpression arg: args)
    		{
    			argvals.add(arg.eval(ctxt));
    		}

			ObjectValue objval = classdef.newInstance(ctordef, argvals, ctxt);

    		if (objval.invlistener != null)
    		{
    			// Check the initial values of the object's fields
    			objval.invlistener.doInvariantChecks = true;
    			objval.invlistener.changedValue(location, objval, ctxt);
    		}

    		return objval;
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNewExpression(this, arg);
	}
}
