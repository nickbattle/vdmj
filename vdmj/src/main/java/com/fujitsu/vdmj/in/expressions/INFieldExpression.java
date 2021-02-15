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
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.FieldMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;

public class INFieldExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression object;
	public final TCIdentifierToken field;
	public final TCNameToken memberName;

	public INFieldExpression(INExpression object, TCIdentifierToken field, TCNameToken memberName)
	{
		super(object);
		this.object = object;
		this.field = field;
		this.field.getLocation().executable(true);
		this.memberName = memberName;
	}

	@Override
	public String toString()
	{
		return "(" + object + "." +
			(memberName == null ? field.getName() : memberName.getName()) + ")";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		this.field.getLocation().hit();

		try
		{
    		return evaluate(ctxt);
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
	}
	
	public Value evaluate(Context ctxt) throws ValueException
	{
		Value v = object.eval(ctxt);
		TCType objtype = null;
		Value r = null;

		if (v.isType(ObjectValue.class))
		{
			ObjectValue ov = v.objectValue(ctxt);
	   		objtype = ov.type;
	   		r = ov.get(memberName, memberName.isExplicit());
		}
		else
		{
			RecordValue rv = v.recordValue(ctxt);
	   		objtype = rv.type;
			FieldMap fields = rv.fieldmap;
     		r = fields.get(field.getName());
		}

		if (r == null)
		{
			ExceptionHandler.abort(location, 4006, "Type " + objtype + " has no field " + field.getName(), ctxt);
		}

		return r;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFieldExpression(this, arg);
	}
}
