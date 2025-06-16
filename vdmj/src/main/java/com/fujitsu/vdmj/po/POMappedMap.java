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

import java.util.Map;

import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.mapper.MappedMap;

abstract public class POMappedMap<FROM_KEY extends Mappable, FROM extends Mappable,
	TO_KEY extends Mappable, TO extends Mappable> extends MappedMap<FROM_KEY, FROM, TO_KEY, TO>
	implements Mappable
{
	private static final long serialVersionUID = 1L;

	public POMappedMap(Map<FROM_KEY, FROM> from) throws Exception
	{
		super(PONode.MAPPINGS, from);
	}
	
	public POMappedMap()
	{
		super();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other instanceof POMappedMap)
		{
			return super.equals(other);
		}
		
		return false;
	}
}
