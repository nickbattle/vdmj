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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.expressions;

import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.Value;

public class INAndExpression extends INBooleanBinaryExpression
{
	private static final long serialVersionUID = 1L;

	public INAndExpression(INExpression left, LexToken op, INExpression right)
	{
		super(left, op, right);
	}

	@Override
	public Value eval(Context ctxt)
	{
		// breakpoint.check(location, ctxt);
		location.hit();		// Mark as covered

		try
		{
			Value lv = left.eval(ctxt);

			if (lv.isUndefined())
			{
				// Use undefinedEval here, because the LHS undefined may *cause* errors that
				// are not real. This happens in POs, where A and B => C, where B may fail because
				// A was not completely evaluated. We have to treat this as MAYBE rather than error.
				
				Value rv = right.undefinedEval(ctxt);

				if (rv.isUndefined())
				{
					return new UndefinedValue();
				}
				else if (rv.boolValue(ctxt))
				{
					return new UndefinedValue();
				}
				else
				{
					return new BooleanValue(false);
				}
			}
			else if (lv.boolValue(ctxt))
			{
				Value rv = right.eval(ctxt);

				if (rv.isUndefined())
				{
					return new UndefinedValue();
				}
				else if (rv.boolValue(ctxt))
				{
					return new BooleanValue(true);
				}
				else
				{
					return new BooleanValue(false);
				}
			}
			else
			{
				return new BooleanValue(false);
			}
		}
		catch (ValueException e)
		{
			return abort(e);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAndExpression(this, arg);
	}
}
