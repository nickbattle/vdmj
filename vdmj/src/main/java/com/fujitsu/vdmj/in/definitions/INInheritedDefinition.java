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

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;

/**
 * A class to hold an inherited definition in VDM++.
 */
public class INInheritedDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INDefinition superdef;
	public final TCNameToken oldname;

	public INInheritedDefinition(TCNameToken localname, INAccessSpecifier accessSpecifier, INDefinition def)
	{
		super(localname.getLocation(), accessSpecifier, localname);

		this.superdef = def;
		this.oldname = localname.getOldName();
	}

	@Override
	public TCType getType()
	{
		return superdef.getType();
	}

	@Override
	public String toString()
	{
		return superdef.toString();
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePairList renamed = new NameValuePairList();

		for (NameValuePair nv: superdef.getNamedValues(ctxt))
		{
			renamed.add(new NameValuePair(
				nv.name.getModifiedName(name.getModule()), nv.value));
		}

		return renamed;
	}

	@Override
	public boolean isFunction()
	{
		return superdef.isFunction();
	}

	@Override
	public boolean isOperation()
	{
		return superdef.isOperation();
	}

	@Override
	public boolean isCallableOperation()
	{
		return superdef.isCallableOperation();
	}

	@Override
	public boolean isInstanceVariable()
	{
		return superdef.isInstanceVariable();
	}

	@Override
	public boolean isTypeDefinition()
	{
		return superdef.isTypeDefinition();
	}

	@Override
	public boolean isValueDefinition()
	{
		return superdef.isValueDefinition();
	}

	@Override
	public boolean isRuntime()
	{
		return superdef.isRuntime();
	}

	@Override
	public boolean isUpdatable()
	{
		return superdef.isUpdatable();
	}

	@Override
	public boolean isSubclassResponsibility()
	{
		return superdef.isSubclassResponsibility();
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseInheritedDefinition(this, arg);
	}
}
