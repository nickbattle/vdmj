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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.annotations;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotation;
import com.fujitsu.vdmj.ast.annotations.ASTAnnotationList;
import com.fujitsu.vdmj.tc.TCMappedList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

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
	}
	
	public void before(TCDefinition def, Environment env, NameScope scope)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.before(def, env, scope);
		}
	}

	public void before(TCModule m)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.before(m);
		}
	}

	public void after(TCDefinition def, TCType type, Environment env, NameScope scope)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.after(def, type, env, scope);
		}
	}

	public void after(TCModule m)
	{
		for (TCAnnotation annotation: this)
		{
			annotation.after(m);
		}
	}
}
