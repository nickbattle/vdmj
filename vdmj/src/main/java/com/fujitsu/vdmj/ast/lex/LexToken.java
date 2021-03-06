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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.ast.lex;

import java.io.Serializable;
import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;

/**
 * The parent class for all lexical token types.
 */
abstract public class LexToken extends ASTNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	/** The textual location of the token. */
	public final LexLocation location;
	
	/** The basic type of the token. */
	public final Token type;

	/**
	 * Create a token of the given type at the given location.
	 *
	 * @param location	The location of the token.
	 * @param type		The basic type of the token.
	 */
	public LexToken(LexLocation location, Token type)
	{
		this.location = location;
		this.type = type;
	}

	/**
	 * Test whether this token is a given basic type.
	 *
	 * @param ttype	The type to test.
	 * @return	True if this is of that type.
	 */
	public boolean is(Token ttype)
	{
		return this.type == ttype;
	}

	/**
	 * Test whether this token is not a given basic type.
	 *
	 * @param ttype	The type to test.
	 * @return	True if this is not of that type.
	 */
	public boolean isNot(Token ttype)
	{
		return this.type != ttype;
	}

	@Override
	public String toString()
	{
		return type.toString();
	}
}
