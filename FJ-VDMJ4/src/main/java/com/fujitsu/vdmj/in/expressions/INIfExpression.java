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
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INIfExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final INExpression ifExp;
	public final INExpression thenExp;
	public final INElseIfExpressionList elseList;
	public final INExpression elseExp;

	public INIfExpression(LexLocation location,
		INExpression ifExp, INExpression thenExp, INElseIfExpressionList elseList, INExpression elseExp)
	{
		super(location);
		this.ifExp = ifExp;
		this.thenExp = thenExp;
		this.elseList = elseList;
		this.elseExp = elseExp;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(if " + ifExp + "\nthen " + thenExp);

		for (INElseIfExpression s: elseList)
		{
			sb.append("\n");
			sb.append(s.toString());
		}

		if (elseExp != null)
		{
			sb.append("\nelse ");
			sb.append(elseExp.toString());
		}

		sb.append(")");

		return sb.toString();
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;
		found = ifExp.findExpression(lineno);
		if (found != null) return found;
		found = thenExp.findExpression(lineno);
		if (found != null) return found;

		for (INElseIfExpression stmt: elseList)
		{
			found = stmt.findExpression(lineno);
			if (found != null) return found;
		}

		if (elseExp != null)
		{
			found = elseExp.findExpression(lineno);
		}

		return found;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		try
		{
    		if (ifExp.eval(ctxt).boolValue(ctxt))
    		{
    			return thenExp.eval(ctxt);
    		}

    		for (INElseIfExpression elseif: elseList)
			{
				Value r = elseif.eval(ctxt);
				if (r != null) return r;
			}

			return elseExp.eval(ctxt);
        }
        catch (ValueException e)
        {
        	return abort(e);
        }
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = ifExp.getValues(ctxt);
		list.addAll(thenExp.getValues(ctxt));

		for (INElseIfExpression elif: elseList)
		{
			list.addAll(elif.getValues(ctxt));
		}

		if (elseExp != null)
		{
			list.addAll(elseExp.getValues(ctxt));
		}

		return list;
	}

	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = ifExp.getOldNames();
		list.addAll(thenExp.getOldNames());

		for (INElseIfExpression elif: elseList)
		{
			list.addAll(elif.getOldNames());
		}

		if (elseExp != null)
		{
			list.addAll(elseExp.getOldNames());
		}

		return list;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIfExpression(this, arg);
	}
}
