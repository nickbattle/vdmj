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
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INIsOfClassExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken classname;
	public final INExpression exp;

	public INIsOfClassExpression(LexLocation location, TCNameToken classname, INExpression exp)
	{
		super(location);
		this.classname = classname.getExplicit(false);
		this.exp = exp;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		classname.getLocation().hit();

		try
		{
			Value v = exp.eval(ctxt).deref();

			if (!(v instanceof ObjectValue))
			{
				return new BooleanValue(false);
			}

			ObjectValue ov = v.objectValue(ctxt);
			return new BooleanValue(isOfClass(ov, classname.getName()));
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	private boolean isOfClass(ObjectValue obj, String name)
	{
		if (obj.type.name.getName().equals(name))
		{
			return true;
		}
		else
		{
			for (ObjectValue objval: obj.superobjects)
			{
				if (isOfClass(objval, name))
				{
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public INExpression findExpression(int lineno)
	{
		INExpression found = super.findExpression(lineno);
		if (found != null) return found;

		return exp.findExpression(lineno);
	}

	@Override
	public String toString()
	{
		return "isofclass(" + classname + "," + exp + ")";
	}

	@Override
	public ValueList getValues(Context ctxt)
	{
		return exp.getValues(ctxt);
	}

	@Override
	public TCNameList getOldNames()
	{
		return exp.getOldNames();
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseIsOfClassExpression(this, arg);
	}
}
