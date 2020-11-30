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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

public class INIfStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INExpression ifExp;
	public final INStatement thenStmt;
	public final INElseIfStatementList elseList;
	public final INStatement elseStmt;

	public INIfStatement(LexLocation location,
		INExpression ifExp, INStatement thenStmt,
		INElseIfStatementList elseList, INStatement elseStmt)
	{
		super(location);
		this.ifExp = ifExp;
		this.thenStmt = thenStmt;
		this.elseList = elseList;
		this.elseStmt = elseStmt;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("if " + ifExp + "\nthen\n" + thenStmt);

		for (INElseIfStatement s: elseList)
		{
			sb.append(s.toString());
		}

		if (elseStmt != null)
		{
			sb.append("else\n");
			sb.append(elseStmt.toString());
		}

		return sb.toString();
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		if (ifExp.eval(ctxt).boolValue(ctxt))
    		{
    			return thenStmt.eval(ctxt);
    		}
    		else
    		{
    			for (INElseIfStatement elseif: elseList)
    			{
    				Value r = elseif.eval(ctxt);
    				if (r != null) return r;
    			}

    			if (elseStmt != null)
    			{
    				return elseStmt.eval(ctxt);
    			}

    			return new VoidValue();
    		}
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIfStatement(this, arg);
	}
}
