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

import com.fujitsu.vdmj.ast.lex.LexQuoteToken;
import com.fujitsu.vdmj.ast.types.visitors.ASTTypeVisitor;

public class ASTQuoteType extends ASTType
{
	private static final long serialVersionUID = 1L;
	public final String value;

	public ASTQuoteType(LexQuoteToken token)
	{
		super(token.location);
		value = token.value;
	}

	@Override
	public String toDisplay()
	{
		return "<" + value + ">";
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ASTQuoteType)
		{
			ASTQuoteType qother = (ASTQuoteType)other;
			return this.value.equals(qother.value);
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		return value.hashCode();
	}

	@Override
	public <R, S> R apply(ASTTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseQuoteType(this, arg);
	}
}
