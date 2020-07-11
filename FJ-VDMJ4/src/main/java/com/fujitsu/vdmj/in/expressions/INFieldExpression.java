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

import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ExceptionHandler;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.FieldMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

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
	
	private Value evaluate(Context ctxt) throws ValueException
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
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return object.findExpression(lineno);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList values = object.getValues(ctxt);
		
		try
		{
			// This evaluation should not affect scheduling as we are trying to
			// discover the sync variables to listen to only.
			
			ctxt.threadState.setAtomic(true);
			Value r = evaluate(ctxt);

			if (r instanceof UpdatableValue)
			{
				values.add(r);
			}
			
			return values;
		}
		catch (ContextException e)
		{
			if (e.number == 4034 || e.number == 4097 || e.number == 4105)
			{
				return values;	// Non existent variable or can't get value
			}
			else
			{
				throw e;
			}
		}
		catch (ValueException e)
		{
			if (e.number == 4097 || e.number == 4105)
			{
				return values;	// Cannot get record/object value of ... 
			}
			else
			{
				abort(e);
				return null;
			}
		}
		finally
		{
			ctxt.threadState.setAtomic(false);
		}
	}

	@Override
	public TCNameList getOldNames()
	{
		return object.getOldNames();
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseFieldExpression(this, arg);
	}
}
