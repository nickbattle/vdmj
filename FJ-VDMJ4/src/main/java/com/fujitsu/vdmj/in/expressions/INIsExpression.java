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

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INIsExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public TCType basictype;
	public final TCNameToken typename;
	public final INExpression test;
	public final INDefinition typedef;

	public INIsExpression(LexLocation location, TCType basictype, TCNameToken typename,
		INExpression test, INDefinition typedef)
	{
		super(location);
		this.basictype = basictype;
		this.typename = typename;
		this.test = test;
		this.typedef = typedef;
	}

	@Override
	public String toString()
	{
		return "is_(" + test + ", " + (typename == null ? basictype : typename) + ")";
	}
	
	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Value v = test.eval(ctxt);

		try
		{
    		if (typename != null)
    		{
    			if (typedef != null)
    			{
    				if (typedef.isTypeDefinition())
    				{
    					// NB. we skip the DTC enabled check here
    					v.convertValueTo(typedef.getType(), ctxt);
    					return new BooleanValue(true);
    				}
    			}
    			else if (v.isType(RecordValue.class))
    			{
    				RecordValue rv = v.recordValue(ctxt);
    				return new BooleanValue(rv.type.name.equals(typename));
    			}
    		}
    		else
    		{
    			// NB. we skip the DTC enabled check here
   				v.convertValueTo(basictype, ctxt);
   				return new BooleanValue(true);
    		}
		}
		catch (ContextException ex)
		{
			if (ex.number != 4060)	// INType invariant violation
			{
				throw ex;	// Otherwise return false
			}
		}
		catch (ValueException ex)
		{
			// return false...
		}

		return new BooleanValue(false);
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return test.findExpression(lineno);
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return test.getValues(ctxt);
	}

	@Override
	public TCNameList getOldNames()
	{
		return test.getOldNames();
	}
}
