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

package smtlib.solvers;

import java.util.List;

import smtlib.ast.Expression;
import smtlib.ast.Sort;
import smtlib.ast.Source;
import smtlib.ast.Text;

public class CVC5Solver extends Solver
{
	@Override
	public String getName()
	{
		return "cvc5";
	}

	@Override
	public ProcessBuilder getProcess()
	{
		return new ProcessBuilder("cvc5", "--lang", "smt2", "--quiet", "--incremental", "--sets-ext");
	}

	@Override
	public Expression setMember(Source e, Source set)
	{
		return new Expression("set.member", e, set);
	}

	@Override
	public Expression setCard(Source set)
	{
		return new Expression(new Text("set.card"), set);
	}

	@Override
	public Expression setUnion(Source s1, Source s2)
	{
		return new Expression("set.union", s1, s2);
	}

	@Override
	public Expression setInter(Source s1, Source s2)
	{
		return new Expression("set.inter", s1, s2);
	}

	@Override
	public Expression setMinus(Source s1, Source s2)
	{
		return new Expression("set.minus", s1, s2);
	}

	@Override
	public Expression setInsert(List<Source> elems)
	{
		Expression result = new Expression("set.insert");

		for (Source elem: elems)
		{
			result.add(elem);	// Includes final set
		}

		return result;
	}

	@Override
	public Sort setType(Source elem)
	{
		return new Sort(new Text("Set"), elem);
	}
}
