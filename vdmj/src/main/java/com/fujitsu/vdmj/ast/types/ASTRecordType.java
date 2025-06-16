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

package com.fujitsu.vdmj.ast.types;

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.util.Utils;

public class ASTRecordType extends ASTInvariantType
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken name;
	public final ASTFieldList fields;
	public final boolean composed;	// Created via "compose R of ... end"

	public ASTRecordType(LexNameToken name, ASTFieldList fields, boolean composed)
	{
		super(name.location);
		this.name = name;
		this.fields = fields;
		this.composed = composed;
	}

	public ASTRecordType(LexLocation location, ASTFieldList fields)
	{
		super(location);
		this.name = new LexNameToken("?", "?", location);
		this.fields = fields;
		this.composed = false;
	}

	public ASTField findField(String tag)
	{
		for (ASTField f: fields)
		{
			if (f.tag.equals(tag))
			{
				return f;
			}
		}

		return null;
	}

	@Override
	public String toDisplay()
	{
		return name.toString();
	}

	@Override
	public String toDetailedString()
	{
		return "compose " + name + " of " + Utils.listToString(fields) + " end";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTRecordType)
		{
			ASTRecordType rother = (ASTRecordType)other;
			return name.equals(rother.name);	// NB. identical
		}

		return false;
	}

	@Override
	public int compareTo(ASTType other)
	{
		if (other instanceof ASTRecordType)
		{
			ASTRecordType rt = (ASTRecordType)other;
    		String n1 = name.toString();
    		String n2 = rt.name.toString();
    		return n1.compareTo(n2);
		}
		else
		{
			return super.compareTo(other);
		}
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public <R, S> R apply(ASTTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseRecordType(this, arg);
	}
}
