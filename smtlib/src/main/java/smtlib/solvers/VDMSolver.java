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

/**
 * Methods implemented by all solvers to provide independent VDM functionality.
 */
public interface VDMSolver
{
	public Expression setMember(Source e, Source set);
	public Expression setCard(Source set);
	public Expression setUnion(Source s1, Source s2);
	public Expression setInter(Source s1, Source s2);
	public Expression setMinus(Source s1, Source s2);
	public Expression setInsert(List<Source> elems);

	public Sort setType(Source elem);
}
