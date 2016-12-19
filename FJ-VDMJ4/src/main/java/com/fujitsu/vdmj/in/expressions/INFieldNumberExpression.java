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

import com.fujitsu.vdmj.ast.lex.LexIntegerToken;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INFieldNumberExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression tuple;
	public final LexIntegerToken field;

	public INFieldNumberExpression(INExpression tuple, LexIntegerToken field)
	{
		super(tuple);
		this.tuple = tuple;
		this.field = field;
		this.field.location.executable(true);
	}

	@Override
	public String toString()
	{
		return "(" + tuple + ".#" + field + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		this.field.location.hit();

		try
		{
    		ValueList fields = tuple.eval(ctxt).tupleValue(ctxt);
    		Value r = fields.get((int)field.value - 1);

    		if (r == null)
    		{
    			ExceptionHandler.abort(location, 4007, "No such field in tuple: #" + field, ctxt);
    		}

    		return r;
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return tuple.findExpression(lineno);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return tuple.getValues(ctxt);
	}

	@Override
	public TCNameList getOldNames()
	{
		return tuple.getOldNames();
	}
}
