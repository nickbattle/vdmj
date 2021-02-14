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

import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.lex.LexIdentifierToken;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;
import com.fujitsu.vdmj.lex.LexLocation;

public class ASTClassType extends ASTType
{
	private static final long serialVersionUID = 1L;
	public final ASTClassDefinition classdef;
	public final LexNameToken name;		// "CLASS`<name>"

	public ASTClassType(LexLocation location, ASTClassDefinition classdef)
	{
		super(location);

		this.classdef = classdef;
		this.name = classdef.name;
	}

	public LexNameToken getMemberName(LexIdentifierToken id)
	{
		return new LexNameToken(name.name, id);
	}

	@Override
	protected String toDisplay()
	{
		return classdef.name.name;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTClassType)
		{
			ASTClassType oc = (ASTClassType)other;
			return name.equals(oc.name);		// NB. name only
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	@Override
	public <R, S> R apply(ASTTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseClassType(this, arg);
	}
}
