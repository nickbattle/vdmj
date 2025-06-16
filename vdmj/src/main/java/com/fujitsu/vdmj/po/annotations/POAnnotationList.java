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

package com.fujitsu.vdmj.po.annotations;

import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

import com.fujitsu.vdmj.po.POMappedList;
import com.fujitsu.vdmj.po.definitions.POClassDefinition;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.modules.POModule;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.annotations.TCAnnotation;
import com.fujitsu.vdmj.tc.annotations.TCAnnotationList;

public class POAnnotationList extends POMappedList<TCAnnotation, POAnnotation>
{
	private static final long serialVersionUID = 1L;
	
	public POAnnotationList()
	{
		super();
	}
	
	public POAnnotationList(TCAnnotationList from) throws Exception
	{
		super(from);
		
		/**
		 * Annotations are MappingOptional, so we remove any nulls here.
		 */
		this.removeIf(new Predicate<POAnnotation>()
		{
			@Override
			public boolean test(POAnnotation a)
			{
				return a == null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T extends POAnnotation> List<T> getInstances(Class<?> type)
	{
		List<T> found = new Vector<T>();
		
		for (POAnnotation instance: this)
		{
			if (type.isAssignableFrom(instance.getClass()))
			{
				found.add((T) instance);
			}
		}
		
		return found;
	}

	@SuppressWarnings("unchecked")
	public <T extends POAnnotation> T getInstance(Class<?> type)
	{
		for (POAnnotation instance: this)
		{
			if (type.isAssignableFrom(instance.getClass()))
			{
				return (T) instance;
			}
		}
		
		return null;
	}

	public ProofObligationList poBefore(PODefinition def, POContextStack ctxt)
	{
		ProofObligationList list = new ProofObligationList();
		
		for (POAnnotation annotation: this)
		{
			list.addAll(annotation.poBefore(def, ctxt));
		}
		
		return list;
	}

	public ProofObligationList poBefore(POModule module)
	{
		ProofObligationList list = new ProofObligationList();
		
		for (POAnnotation annotation: this)
		{
			list.addAll(annotation.poBefore(module));
		}
		
		return list;
	}

	public ProofObligationList poBefore(POClassDefinition clazz)
	{
		ProofObligationList list = new ProofObligationList();
		
		for (POAnnotation annotation: this)
		{
			list.addAll(annotation.poBefore(clazz));
		}
		
		return list;
	}

	public void poAfter(PODefinition def, ProofObligationList obligations, POContextStack ctxt)
	{
		for (POAnnotation annotation: this)
		{
			annotation.poAfter(def, obligations, ctxt);
		}
	}

	public void poAfter(POModule module, ProofObligationList obligations)
	{
		for (POAnnotation annotation: this)
		{
			annotation.poAfter(module, obligations);
		}
	}

	public void poAfter(POClassDefinition clazz, ProofObligationList obligations)
	{
		for (POAnnotation annotation: this)
		{
			annotation.poAfter(clazz, obligations);
		}
	}
}
