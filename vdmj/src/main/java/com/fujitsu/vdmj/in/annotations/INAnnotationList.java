/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.in.annotations;

import java.util.function.Predicate;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;

public class INAnnotationList extends INMappedList<TCAnnotation, INAnnotation>
{
	private static final long serialVersionUID = 1L;
	
	public INAnnotationList()
	{
		super();
	}
	
	public INAnnotationList(TCAnnotationList from) throws Exception
	{
		super(from);
		
		/**
		 * Annotations are MappingOptional, so we remove any nulls here.
		 */
		this.removeIf(new Predicate<INAnnotation>()
		{
			@Override
			public boolean test(INAnnotation a)
			{
				return a == null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T extends INAnnotation> T getInstance(Class<?> type)
	{
		for (INAnnotation instance: this)
		{
			if (type.isAssignableFrom(instance.getClass()))
			{
				return (T) instance;
			}
		}
		
		return null;
	}
}
