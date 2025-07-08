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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.annotations;

import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;
import com.fujitsu.vdmj.values.Value;

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

	@SuppressWarnings("unchecked")
	public <T extends INAnnotation> List<T> getInstances(Class<?> type)
	{
		List<T> found = new Vector<T>();

		for (INAnnotation instance: this)
		{
			if (type.isAssignableFrom(instance.getClass()))
			{
				found.add((T) instance);
			}
		}
		
		return found;
	}

	public void inBefore(INStatement stmt, Context ctxt)
	{
		for (INAnnotation annotation: this)
		{
			annotation.inBefore(stmt, ctxt);
		}
	}

	public void inAfter(INStatement stmt, Value rv, Context ctxt)
	{
		for (INAnnotation annotation: this)
		{
			annotation.inAfter(stmt, rv, ctxt);
		}
	}

	public void inBefore(INExpression second, Context ctxt)
	{
		for (INAnnotation annotation: this)
		{
			annotation.inBefore(second, ctxt);
		}
	}

	public void inAfter(INExpression second, Value rv, Context ctxt)
	{
		for (INAnnotation annotation: this)
		{
			annotation.inAfter(second, rv, ctxt);
		}
	}
}
