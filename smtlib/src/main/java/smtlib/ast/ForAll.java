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

public class ForAll extends Expression
{
	public ForAll(Bracketed binds, Expression predictate)
	{
		super(new Text("forall"), binds, predictate);
	}

	@Override
	public String toFormat(int indent)
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(" ".repeat(indent));
		sb.append("(forall ");
		sb.append(get(1).toSource());
		sb.append("\n");
		sb.append(get(2).toFormat(indent + 2));
		sb.append("\n");
		sb.append(" ".repeat(indent));
		sb.append(")");

		return sb.toString();
	}
}
