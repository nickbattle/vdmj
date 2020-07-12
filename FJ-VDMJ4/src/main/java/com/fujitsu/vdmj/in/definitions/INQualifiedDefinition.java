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
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.NameValuePairList;

public class INQualifiedDefinition extends INDefinition
{
	private static final long serialVersionUID = 1L;
	public final INDefinition def;
	public final TCType type;

	public INQualifiedDefinition(INDefinition qualifies, TCType type)
	{
		super(qualifies.location, qualifies.accessSpecifier, qualifies.name);
		this.def = qualifies;
		this.type = type;
	}

	public INQualifiedDefinition(INDefinition qualifies)
	{
		super(qualifies.location, qualifies.accessSpecifier, qualifies.name);
		this.def = qualifies;
		this.type = qualifies.getType();
	}

	@Override
	public String toString()
	{
		return def.toString();
	}

	@Override
	public boolean equals(Object other)
	{
		return def.equals(other);
	}

	@Override
	public int hashCode()
	{
		return def.hashCode();
	}

	@Override
	public TCNameList getOldNames()
	{
		return def.getOldNames();
	}

	@Override
	public TCType getType()
	{
		return type; // NB. Not delegated!
	}

	@Override
	public NameValuePairList getNamedValues(Context ctxt)
	{
		return def.getNamedValues(ctxt);
	}

	@Override
	public boolean isAccess(Token kind)
	{
		return def.isAccess(kind);
	}

	@Override
	public boolean isStatic()
	{
		return def.isStatic();
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
	public boolean isCallableFunction()
	{
		return def.isCallableFunction();
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
		return super.isUpdatable();		// Note, not delegated
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseQualifiedDefinition(this, arg);
	}
}
