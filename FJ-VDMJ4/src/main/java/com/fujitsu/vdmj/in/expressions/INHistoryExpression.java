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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.ClassContext;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ObjectContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INHistoryExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final Token hop;
	public final TCNameList opnames;

	public INHistoryExpression(
		LexLocation location, Token hop, TCNameList list)
	{
		super(location);
		this.hop = hop;
		this.opnames = list;
	}

	@Override
	public Value eval(Context ctxt)
	{
		try
		{
			// TODO Not very efficient to do this every time. But we can't
			// save the list because the same TCHistoryExpression is called from
			// different object instance contexts, and each instance has its
			// own operation history counters...

			ValueList operations = new ValueList();
			
			if (ctxt instanceof ObjectContext)
			{
				ObjectValue self = ((ObjectContext)ctxt).self;
	
				for (TCNameToken opname: opnames)
				{
					operations.addAll(self.getOverloads(opname));
				}
			}
			else if (ctxt instanceof ClassContext)
			{
				ClassContext cctxt = (ClassContext)ctxt;
				Context statics = cctxt.classdef.getStatics();
				
				for (TCNameToken opname: opnames)
				{
					for (TCNameToken sname: statics.keySet())
					{
						if (opname.matches(sname))
						{
							operations.add(ctxt.check(sname));
						}
					}
				}
			}
			
			if (operations.isEmpty())
			{
				abort(4011, "Illegal history operator: " + hop, ctxt);
			}

			int result = 0;

    		for (Value v: operations)
    		{
    			OperationValue ov = v.operationValue(ctxt);

    			switch (hop)
    			{
    				case ACT:
    					result += ov.hashAct;
    					break;

    				case FIN:
       					result += ov.hashFin;
    					break;

    				case REQ:
       					result += ov.hashReq;
    					break;

    				case ACTIVE:
       					result += ov.hashAct - ov.hashFin;
    					break;

    				case WAITING:
       					result += ov.hashReq - ov.hashAct;
    					break;

    				default:
    					abort(4011, "Illegal history operator: " + hop, ctxt);

    			}
    		}

    		location.hit();
    		return new NaturalValue(result);
		}
		catch (ValueException e)
		{
			return abort(e);
		}
		catch (ContextException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			return abort(4065, e.getMessage(), ctxt);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append(hop.toString().toLowerCase());
		sb.append("(");
		String sep = "";

		for (TCNameToken opname: opnames)
		{
			sb.append(sep);
			sep = ", ";
			sb.append(opname.getName());
		}

		sb.append(")");
		return sb.toString();
	}
}
