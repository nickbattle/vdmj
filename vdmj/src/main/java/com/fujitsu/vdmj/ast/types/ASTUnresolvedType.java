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

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;

public class ASTUnresolvedType extends ASTType
{
	private static final long serialVersionUID = 1L;
	public final LexNameToken typename;

	public ASTUnresolvedType(LexNameToken typename)
	{
		super(typename.location);
		this.typename = typename;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTUnresolvedType)
		{
			ASTUnresolvedType nother = (ASTUnresolvedType)other;
			return typename.equals(nother.typename);
		}

		if (other instanceof ASTNamedType)
		{
			ASTNamedType nother = (ASTNamedType)other;
			return typename.equals(nother.typename);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return typename.hashCode();
	}

	@Override
	public String toDisplay()
	{
		return "(unresolved " + typename + ")";
	}

	@Override
	public <R, S> R apply(ASTTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseUnresolvedType(this, arg);
	}
}
