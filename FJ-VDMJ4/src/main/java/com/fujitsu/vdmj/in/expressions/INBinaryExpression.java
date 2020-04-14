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

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.values.ValueList;

abstract public class INBinaryExpression extends INExpression
{
	private static final long serialVersionUID = 1L;

	public final INExpression left;
	public final INExpression right;
	public final LexToken op;

	public INBinaryExpression(INExpression left, LexToken op, INExpression right)
	{
		super(op.location);
		this.left = left;
		this.right = right;
		this.op = op;
	}

	@Override
	public INExpression findExpression(int lineno)
	{
//		TCExpression found = super.findExpression(lineno);
//		if (found != null) return found;

		INExpression found = left.findExpression(lineno);
		if (found != null) return found;

		return right.findExpression(lineno);
	}

	@Override
	public String toString()
	{
		return "(" + left + " " + op + " " + right + ")";
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		ValueList list = left.getValues(ctxt);
		list.addAll(right.getValues(ctxt));
		return list;
	}
	
	@Override
	public TCNameList getOldNames()
	{
		TCNameList list = left.getOldNames();
		list.addAll(right.getOldNames());
		return list;
	}

	@Override
	public INExpressionList getSubExpressions()
	{
		INExpressionList subs = left.getSubExpressions();
		subs.addAll(right.getSubExpressions());
		subs.add(this);
		return subs;
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBinaryExpression(this, arg);
	}
}
