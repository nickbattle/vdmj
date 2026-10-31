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

public class Sort extends SExp
{
	// 〈sort〉 ::= 〈identifer〉 | ( 〈identifer〉 〈sort〉+ )

	public Sort(Source... parts)
	{
		super(parts);
	}

	public Sort(String type)
	{
		super(new Text(type));
	}

	public Sort(String type, String elem)
	{
		super(new Text(type), new Text(elem));
	}

	public Sort(String type, Source elem)
	{
		super(new Text(type), elem);
	}

	public Expression undefinedConst()
	{
		String type = get(0).toString();

		switch (type)
		{
			case "Int":		return new Expression("0");
			case "Bool":	return new Expression("false");
			case "Real":	return new Expression("0.0");

			default:
				throw new UnsupportedOperationException("Missing undefinedConst for: " + type);
		}
	}
}
