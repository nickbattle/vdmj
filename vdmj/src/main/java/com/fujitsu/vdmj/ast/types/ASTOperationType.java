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

import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.util.Utils;

public class ASTOperationType extends ASTType
{
	private static final long serialVersionUID = 1L;
	public final ASTTypeList parameters;
	public final ASTType result;
	public boolean pure;

	public ASTOperationType(LexLocation location, ASTTypeList parameters, ASTType result)
	{
		super(location);
		this.parameters = parameters;
		this.result = result;
		this.pure = false;
	}

	public ASTOperationType(LexLocation location)	// Create "() ==> ()"
	{
		super(location);
		this.parameters = new ASTTypeList();
		this.result = new ASTVoidType(location);
		this.pure = false;
	}

	public boolean isPure()
	{
		return pure;
	}
	
	public void setPure(boolean p)
	{
		this.pure = p;
	}

	@Override
	public String toDisplay()
	{
		String params = (parameters.isEmpty() ?
						"()" : Utils.listToString(parameters, " * "));
		return "(" + params + " ==> " + result + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof ASTOperationType))
		{
			return false;
		}

		ASTOperationType oother = (ASTOperationType)other;
		return (result.equals(oother.result) &&
				parameters.equals(oother.parameters));
	}

	@Override
	public int hashCode()
	{
		return parameters.hashCode() + result.hashCode();
	}

	@Override
	public <R, S> R apply(ASTTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseOperationType(this, arg);
	}
}
