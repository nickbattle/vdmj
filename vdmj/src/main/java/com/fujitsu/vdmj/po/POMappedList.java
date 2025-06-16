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

package com.fujitsu.vdmj.po;

import java.util.List;

import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.mapper.MappedList;

abstract public class POMappedList<FROM extends Mappable, TO extends Mappable> extends MappedList<FROM, TO>
{
	private static final long serialVersionUID = 1L;

	public POMappedList(List<FROM> from) throws Exception
	{
		super(PONode.MAPPINGS, from);
	}
	
	public POMappedList()
	{
		super();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof POMappedList)
		{
			return super.equals(other);
		}
		
		return false;
	}
}
