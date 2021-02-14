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

package com.fujitsu.vdmj.ast.types;

import java.io.Serializable;

import com.fujitsu.vdmj.ast.ASTNode;
import com.fujitsu.vdmj.ast.definitions.ASTAccessSpecifier;
import com.fujitsu.vdmj.ast.lex.LexNameToken;

public class ASTField extends ASTNode implements Serializable
{
	private static final long serialVersionUID = 1L;

	public final ASTAccessSpecifier accessibility;
	public final LexNameToken tagname;
	public final String tag;
	public final ASTType type;
	public final boolean equalityAbstraction;

	public ASTField(LexNameToken tagname, String tag, ASTType type, boolean equalityAbstraction)
	{
		this.accessibility = null;
		this.tagname = tagname;
		this.tag = tag;
		this.type = type;
		this.equalityAbstraction = equalityAbstraction;
	}

	@Override
	public String toString()
	{
		return tagname + (equalityAbstraction ? ":-" : ":") + type;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTField)
		{
			return toString().equals(other.toString());	// Includes equality abstraction
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return tag.hashCode() + type.hashCode();
	}
}
