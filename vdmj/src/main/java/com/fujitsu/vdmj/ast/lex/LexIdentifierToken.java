/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.ast.lex;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;

public class LexIdentifierToken extends LexToken
{
	private static final long serialVersionUID = 1L;
	public final String name;
	public final boolean old;

	public LexIdentifierToken(String name, boolean old, LexLocation location)
	{
		super(location, Token.IDENTIFIER);
		this.name = name;
		this.old = old;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof LexIdentifierToken)
		{
			LexIdentifierToken tother = (LexIdentifierToken)other;
			return this.name.equals(tother.name) && this.old == tother.old;
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode() + (old ? 1 : 0);
	}

	@Override
	public String toString()
	{
		return name + (old ? "~" : "");
	}

	public LexNameToken getClassName()
	{
		// We don't know the class name of the name of a class until we've
		// read the name. So create a new location with the right module.

		LexLocation loc = new LexLocation(
			location.file,
			name,
			location.startLine,
			location.startPos,
			location.endLine,
			location.endPos);

		return new LexNameToken("CLASS", name, loc);
	}
}
