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

import smtlib.ast.And;
import smtlib.ast.Expression;
import smtlib.ast.ExpressionSet;
import smtlib.ast.Implies;

public class SMTPair
{
	public final Expression value;
	public final ExpressionSet deltas;

	public SMTPair(Expression value, Expression delta)
	{
		this.value = value;
		this.deltas = new ExpressionSet(delta);
	}

	public SMTPair(Expression value)
	{
		this.value = value;
		this.deltas = new ExpressionSet();
	}

	public SMTPair(Expression value, ExpressionSet deltas)
	{
		this.value = value;
		this.deltas = deltas;
	}

	public SMTPair andDeltas()
	{
		if (deltas.isEmpty())
		{
			return this;
		}
		else
		{
			return new SMTPair(new And(deltas.andOf(), value));
		}
	}

	public SMTPair impliedBy(ExpressionSet clauses)
	{
		if (clauses.isEmpty())
		{
			return this;
		}
		else
		{
			return new SMTPair(new Implies(clauses.andOf(), value));
		}
	}

	public SMTPair andWith(ExpressionSet clauses)
	{
		if (clauses.isEmpty())
		{
			return this;
		}
		else
		{
			return new SMTPair(new And(clauses.andOf(), value));
		}
	}

	@Override
	public String toString()
	{
		if (deltas.isEmpty())
		{
			return value.toSource();
		}
		else
		{
			return deltas.andOf().toSource() + " => " + value.toSource();
		}
	}
}
