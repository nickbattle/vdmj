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

import java.util.Vector;

public abstract class SExp extends Vector<Source> implements Source
{
	protected boolean oneLine = false;

	public SExp(Source... sources)
	{
		for (Source src: sources)
		{
			this.add(src);
		}
	}

	@Override
	public String toSource()
	{
		if (size() == 1)
		{
			return get(0).toSource();
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			String sep = "";

			for (Source arg: this)
			{
				sb.append(sep);
				sb.append(arg.toSource());
				sep = " ";
			}

			sb.append(")");
			return sb.toString();
		}
	}

	@Override
	public String toFormat(int indent)
	{
		if (oneLine)
		{
			return " ".repeat(indent) + toSource();
		}
		else if (size() == 1)
		{
			return get(0).toFormat(indent);
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append(" ".repeat(indent));
			sb.append("(\n");

			for (Source arg: this)
			{
				sb.append(arg.toFormat(indent + 2));
				sb.append("\n");
			}

			sb.append(" ".repeat(indent));
			sb.append(")");
			return sb.toString();
		}
	}
}
