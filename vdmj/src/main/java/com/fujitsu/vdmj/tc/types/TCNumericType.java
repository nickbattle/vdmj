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

public abstract class TCNumericType extends TCBasicType
{
	private static final long serialVersionUID = 1L;

	public TCNumericType(LexLocation location)
	{
		super(location);
	}

	public abstract int getWeight();

	@Override
	public boolean isNumeric(LexLocation from)
	{
		return true;
	}

	@Override
	public TCNumericType getNumeric()
	{
		return this;
	}
	
	public static TCNumericType typeOf(long iv, LexLocation location)
	{
		if (iv > 0)
		{
			return new TCNaturalOneType(location);
		}
		else if (iv >= 0)
		{
			return new TCNaturalType(location);
		}
		else
		{
			return new TCIntegerType(location);
		}
	}
	
	@Override
	public boolean isOrdered(LexLocation from)
	{
		return true;
	}
}