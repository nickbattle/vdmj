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

package com.fujitsu.vdmj.tc.definitions;

import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;

/**
 * A class to hold a renamed import definition.
 */
public class TCRenamedDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCDefinition def;

	public TCRenamedDefinition(TCNameToken name, TCDefinition def)
	{
		super(def.pass, name.getLocation(), name,
			def instanceof TCStateDefinition? NameScope.TYPENAME : def.nameScope);
		this.def = def;
	}

	@Override
	public TCType getType()
	{
		return def.getType();
	}

	@Override
	public String toString()
	{
		return def + " renamed " + name;
	}
	
	@Override
	public String kind()
	{
		return def.kind();
	}

	@Override
	public void typeResolve(Environment env)
	{
		def.typeResolve(env);
	}

	@Override
	public void typeCheck(Environment base, NameScope scope)
	{
		def.typeCheck(base, scope);
	}

	@Override
    public void markUsed()
	{
		used = true;
		def.markUsed();
	}

//	@Override
//    protected boolean isUsed()
//	{
//		return def.isUsed();
//	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(this);
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		TCDefinition renamed = super.findName(sought, scope);

		if (renamed != null)
		{
			def.markUsed();
			return renamed;
		}
		else
		{
			// Renamed definitions hide the original name
			return null; // def.findName(sought, scope);
		}
	}

	@Override
	public TCDefinition findType(TCNameToken sought, String fromModule)
	{
		// We can only find an import if it is being sought from the module that
		// imports it.

		if (fromModule != null && !location.module.equals(fromModule))
		{
			return null;	// Someone else's import
		}

		TCDefinition renamed = super.findName(sought, NameScope.TYPENAME);

		if (renamed != null && (def instanceof TCTypeDefinition || def instanceof TCStateDefinition))
		{
			def.markUsed();
			return renamed;
		}
		else
		{
			return def.findType(sought, fromModule);
		}
	}

	@Override
	public boolean isFunction()
	{
		return def.isFunction();
	}

	@Override
	public boolean isOperation()
	{
		return def.isOperation();
	}

	@Override
	public boolean isCallableOperation()
	{
		return def.isCallableOperation();
	}

	@Override
	public boolean isInstanceVariable()
	{
		return def.isInstanceVariable();
	}

	@Override
	public boolean isTypeDefinition()
	{
		return def.isTypeDefinition();
	}

	@Override
	public boolean isValueDefinition()
	{
		return def.isValueDefinition();
	}

	@Override
	public boolean isRuntime()
	{
		return def.isRuntime();
	}

	@Override
	public boolean isUpdatable()
	{
		return def.isUpdatable();
	}

	@Override
	public TCDefinition deref()
	{
		return def.deref();
	}

	@Override
	public <R, S> R apply(TCDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseRenamedDefinition(this, arg);
	}
}
