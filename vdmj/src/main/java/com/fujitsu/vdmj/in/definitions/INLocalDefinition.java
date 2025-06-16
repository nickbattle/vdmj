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
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;

/**
 * A class to hold a local variable definition.
 */
public class INLocalDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCType type;
	public final INValueDefinition valueDefinition;

	public INLocalDefinition(LexLocation location, TCNameToken name, TCType type, INValueDefinition valueDefinition)
	{
		super(location, null, name);
		this.type = type;
		this.valueDefinition = valueDefinition;
	}

	@Override
	public String toString()
	{
		return name.getName() + " = " + type;
	}

	@Override
	public TCType getType()
	{
		return type == null ? new TCUnknownType(location) : type;
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		NameValuePair nvp = new NameValuePair(name, ctxt.lookup(name));
		return new NameValuePairList(nvp);
	}

	@Override
	public boolean isFunction()
	{
		// This is only true for local definitions of member functions or
		// operations, not local definitions that happen to be function values.
		// So we exclude parameter types. We also exclude value definitions.

		return (valueDefinition != null ||
			type.isType(TCParameterType.class, location)) ? false : type.isFunction(location);
	}

	@Override
	public boolean isUpdatable()
	{
		return getType().isClass(null);
	}

	@Override
	public boolean isValueDefinition()
	{
		return valueDefinition != null;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseLocalDefinition(this, arg);
	}
}
