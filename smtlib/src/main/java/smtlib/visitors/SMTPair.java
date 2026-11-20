/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package smtlib.visitors;

import java.util.Arrays;

import smtlib.ast.And;
import smtlib.ast.Expression;

public class SMTPair
{
	public final Expression value;
	public final Expression delta;

	public SMTPair(Expression value, Expression delta)
	{
		this.value = value;
		this.delta = delta;
	}

	public SMTPair(Expression value)
	{
		this(value, null);
	}

	public static Expression deltas(Object... terms)
	{
		return deltas(Arrays.asList(terms));
	}

	public static Expression deltas(Iterable<Object> terms)
	{
		Expression result = null;

		for (Object term: terms)
		{
			Expression exp = null;

			if (term == null)
			{
				continue;
			}
			else if (term instanceof Expression)
			{
				exp = (Expression)term;
			}
			else if (term instanceof SMTPair)
			{
				SMTPair pair = (SMTPair)term;
				exp = pair.delta;
			}

			if (exp == null)
			{
				continue;
			}

			if (result == null)
			{
				result = exp;
			}
			else
			{
				result = new And(result, exp);
			}
		}

		return result;
	}
}
