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

package com.fujitsu.vdmj.tc.definitions;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.tc.TCNode;

/**
 * A class to represent a [pure][static] public/private/protected specifier.
 */
public class TCAccessSpecifier extends TCNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final static TCAccessSpecifier DEFAULT = new TCAccessSpecifier(false, false, Token.PRIVATE, false);

	public final boolean isStatic;
	public final boolean isAsync;
	public final Token access;
	public final boolean isPure;

	public TCAccessSpecifier(boolean isStatic, boolean isAsync, Token access, boolean pure)
	{
		this.isStatic = isStatic;
		this.isAsync = isAsync;
		this.access = access;
		this.isPure = pure;
	}

	public TCAccessSpecifier getStatic(boolean asStatic)
	{
		return new TCAccessSpecifier(asStatic, isAsync, access, isPure);
	}

	public boolean narrowerThan(TCAccessSpecifier other)
	{
		return narrowerThan(other.access);
	}

	public boolean narrowerThan(Token other)
	{
		switch (access)
		{
			case PRIVATE:
				return other != Token.PRIVATE;

			case PROTECTED:
				return other == Token.PUBLIC;

			case PUBLIC:
			default:
				return false;
		}
	}

	@Override
	public String toString()
	{
		return (isPure? "pure " : "") + (isAsync? "async " : "") + (isStatic ? "static " : "") + access;
	}

	public String ifSet(String sep)
	{
		return (this == DEFAULT) ? "" : (toString() + sep);
	}
}
