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

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;

public class TCQuoteType extends TCType
{
	private static final long serialVersionUID = 1L;
	public final String value;

	public TCQuoteType(LexLocation location, String value)
	{
		super(location);
		this.value = value;
	}

	@Override
	public String toDisplay()
	{
		return "<" + value + ">";
	}

	@Override
	public boolean equals(Object other)
	{
		other = deBracket(other);

		if (other instanceof TCQuoteType)
		{
			TCQuoteType qother = (TCQuoteType)other;
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
	public <R, S> R apply(TCTypeVisitor<R, S> visitor, S arg)
	{
		return visitor.caseQuoteType(this, arg);
	}
}
