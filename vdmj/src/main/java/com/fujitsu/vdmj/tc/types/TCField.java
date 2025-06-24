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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.ast.definitions.ASTAccessSpecifier;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;

public class TCField extends TCNode
{
	private static final long serialVersionUID = 1L;

	public final ASTAccessSpecifier accessibility;
	public final TCNameToken tagname;
	public final String tag;
	public TCType type;
	public final boolean equalityAbstraction;

	public TCField(TCNameToken tagname, String tag, TCType type, boolean equalityAbstraction)
	{
		this.accessibility = null;
		this.tagname = tagname;
		this.tag = tag;
		this.type = type;
		this.equalityAbstraction = equalityAbstraction;
	}

	public void unResolve()
	{
		type.unResolve();
	}

	public void typeResolve(Environment env)
	{
		// Recursion defence done by the type
		type = type.typeResolve(env);

		if (env.isVDMPP())
		{
			if (type instanceof TCFunctionType)
			{
    			tagname.setTypeQualifier(((TCFunctionType)type).parameters);
			}
			else if (type instanceof TCOperationType)
    		{
    			tagname.setTypeQualifier(((TCOperationType)type).parameters);
    		}
		}
	}

	@Override
	public String toString()
	{
		return tagname + (equalityAbstraction ? ":-" : ":") + type;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof TCField)
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
