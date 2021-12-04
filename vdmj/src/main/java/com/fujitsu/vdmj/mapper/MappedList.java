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

package com.fujitsu.vdmj.mapper;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.mapper.ClassMapper;

abstract public class MappedList<FROM extends Mappable, TO extends Mappable> extends Vector<TO> implements Mappable
{
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unchecked")
	public MappedList(String mappings, List<FROM> from) throws Exception
	{
		ClassMapper mapper = ClassMapper.getInstance(mappings);	// NB. no init!
		
		for (FROM type: from)
		{
			add((TO)mapper.convert(type));
		}
	}
	
	public MappedList()
	{
		super();
	}
}
