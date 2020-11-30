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
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.Value;

public class INNarrowExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final TCType basictype;
	public final TCNameToken typename;
	public final INDefinition typedef;
	public final INExpression test;

	public INNarrowExpression(LexLocation location, TCType basictype, TCNameToken typename, INDefinition typedef, INExpression test)
	{
		super(location);
		this.basictype = basictype;
		this.typename = typename;
		this.typedef = typedef;
		this.test = test;
	}

	@Override
	public String toString()
	{
		return "narrow_(" + test + ", " + (typename == null ? basictype : typename) + ")";
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
				if (typedef.isTypeDefinition())
				{
					// NB. we skip the DTC enabled check here
					v = v.convertValueTo(typedef.getType(), ctxt);
				}
    			else if (v.isType(RecordValue.class))
    			{
    				v = v.recordValue(ctxt);
    			}
    		}
    		else
    		{
    			// NB. we skip the DTC enabled check here
   				v = v.convertValueTo(basictype, ctxt);
    		}
		}
		catch (ValueException ex)
		{
			abort(ex);
		}
		
		return v;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNarrowExpression(this, arg);
	}
}
