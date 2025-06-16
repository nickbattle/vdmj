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

import java.util.concurrent.atomic.AtomicBoolean;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.definitions.visitors.TCDefinitionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.Pass;

/**
 * A class to hold an imported definition.
 */
public class TCImportedDefinition extends TCDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCDefinition def;
	private final AtomicBoolean importAllUsed;

	public TCImportedDefinition(LexLocation location, TCDefinition def, AtomicBoolean importAllUsed)
	{
		super(Pass.DEFS, location, def.name, def.nameScope);
		this.def = def;
		this.importAllUsed = importAllUsed;
	}

	public TCImportedDefinition(LexLocation location, TCDefinition def)
	{
		this(location, def, null);
	}

	@Override
	public String toString()
	{
		return def.toString();
	}
	
	@Override
	public String kind()
	{
		return "import";
	}

	@Override
	public TCType getType()
	{
		return def.getType();
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
		
		if (importAllUsed != null)
		{
			importAllUsed.set(true);
		}
	}
	
	@Override
	public boolean isUsed()
	{
		if (importAllUsed != null)
		{
			return importAllUsed.get();
		}
		else
		{
			return used;
		}
	}
	
	@Override
	public void unusedCheck()
	{
		if (importAllUsed != null)
		{
			if (!isUsed())
			{
				warning(5000, "Imports from '" + def.location.module + "' are not used");
				markUsed();		// To avoid multiple warnings
			}
		}
		else
		{
			super.unusedCheck();
		}
	}

	@Override
	public TCDefinitionList getDefinitions()
	{
		return new TCDefinitionList(def);
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

		TCDefinition d = def.findType(sought, fromModule);

		if (d != null)
		{
			markUsed();
		}

		return d;
	}

	@Override
	public TCDefinition findName(TCNameToken sought, NameScope scope)
	{
		TCDefinition d = def.findName(sought, scope);

		if (d != null)
		{
			markUsed();
		}

		return d;
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
		return visitor.caseImportedDefinition(this, arg);
	}
}
