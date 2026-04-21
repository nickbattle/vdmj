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

public class Identifier extends Vector<String> implements Source
{
	// 〈identifer 〉 ::= 〈symbol〉 | ( _ 〈symbol〉 〈index〉+ )

	public Identifier(String... args)
	{
		for (String arg: args)
		{
			this.add(arg);
		}
	}

	@Override
	public String toSource()
	{
		if (size() == 1)
		{
			return get(0);
		}
		else
		{
			StringBuilder sb = new StringBuilder();
			sb.append("(");
			String sep = "";

			for (String arg: this)
			{
				sb.append(sep);
				sb.append(arg);
				sep = " ";
			}

			sb.append(")");
			return sb.toString();
		}
	}
}
