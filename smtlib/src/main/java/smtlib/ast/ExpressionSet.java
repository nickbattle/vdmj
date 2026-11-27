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

package smtlib.ast;

import java.util.HashSet;

import smtlib.visitors.SMTHelper;

public class ExpressionSet extends HashSet<Expression> implements SMTHelper
{
	public  ExpressionSet()
	{
		// empty
	}

	public ExpressionSet(Expression... terms)
	{
		for (Expression term: terms)
		{
			this.add(term);
		}
	}

	public ExpressionSet(ExpressionSet... terms)
	{
		for (ExpressionSet term: terms)
		{
			this.addAll(term);
		}
	}

	public ExpressionSet(Object... terms)
	{
		for (Object term: terms)
		{
			if (term instanceof Expression)
			{
				this.add((Expression)term);
			}
			else if (term instanceof ExpressionSet)
			{
				this.addAll((ExpressionSet)term);
			}
		}
	}

	@Override
	public boolean add(Expression e)
	{
		return (e != null) ? super.add(e) : false;
	}

	public Expression andOf()
	{
		if (isEmpty())
		{
			return TRUE;
		}
		else if (size() == 1)
		{
			return iterator().next();
		}
		else
		{
			return new And(this);
		}
	}

	public Expression orOf()
	{
		if (isEmpty())
		{
			return TRUE;
		}
		else if (size() == 1)
		{
			return iterator().next();
		}
		else
		{
			return new Or(this);
		}
	}
}
