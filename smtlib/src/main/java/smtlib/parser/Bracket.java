/*******************************************************************************
 *
 *	Copyright (c) 2026 Fujitsu Services Ltd.
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

package smtlib.parser;

import java.util.Vector;

public class Bracket extends Vector<Object>
{
	public Bracket getb(int index)
	{
		return (Bracket)get(index);
	}

	public String gets(int index)
	{
		return (String)get(index).toString();	// ie. Brackets get stringified
	}

	@Override
	public String toString()
	{
		Object first = get(0);

		if (first instanceof String)
		{
			String str = (String)first;

			switch (str)
			{
				case "*":
				case "/":
				case "+":
				case "<":
				case "<=":
				case ">":
				case ">=":
				case "=":
				case "and":
				case "or":
				case "=>":
					return "(" + get(1) + str + get(2) + ")";

				case "-":
				case "not":
					if (size() == 2)
					{
						return "(" + str + " " + get(1) + ")";
					}
					else
					{
						return "(" + get(1) + str + get(2) + ")";
					}

				// Drop through...
			}
		}

		StringBuilder sb = new StringBuilder();
		sb.append("(");
		String sep = "";

		for (int i=0; i<size(); i++)
		{
			sb.append(sep);
			sb.append(get(i).toString());
			sep = " ";
		}

		sb.append(")");
		return sb.toString();
	}
}
