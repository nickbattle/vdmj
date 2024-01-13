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

package com.fujitsu.vdmj.tc.annotations;

import java.util.function.Predicate;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;

public class TCAnnotationList extends TCMappedList<ASTAnnotation, TCAnnotation>
{
	private static final long serialVersionUID = 1L;
	
	public TCAnnotationList()
	{
		super();
	}
	
	public TCAnnotationList(ASTAnnotationList from) throws Exception
	{
		super(from);
		
		/**
		 * Annotations are MappingOptional, so we remove any nulls here.
		 */
		this.removeIf(new Predicate<TCAnnotation>()
		{
			@Override
			public boolean test(TCAnnotation a)
			{
				return a == null;
			}
		});
	}
	
	public void tcBefore(TCDefinition def, Environment env, NameScope scope)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcBefore(def, env, scope);
		}
	}

	public void tcBefore(TCModule m)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcBefore(m);
		}
	}

	public void tcBefore(TCModule m, ModuleEnvironment e)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcBefore(m, e);
		}
	}

	public void tcBefore(TCClassDefinition clazz)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcBefore(clazz);
		}
	}

	public void tcBefore(TCClassDefinition clazz, PrivateClassEnvironment self)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcBefore(clazz, self);
		}
	}

	public void tcAfter(TCDefinition def, TCType type, Environment env, NameScope scope)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcAfter(def, type, env, scope);
		}
	}

	public void tcAfter(TCModule m)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcAfter(m);
		}
	}

	public void tcAfter(TCModule m, ModuleEnvironment e)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcAfter(m);
		}
	}

	public void tcAfter(TCClassDefinition clazz)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcAfter(clazz);
		}
	}

	public void tcAfter(TCClassDefinition clazz, PrivateClassEnvironment self)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.tcAfter(clazz, self);
		}
	}
}
