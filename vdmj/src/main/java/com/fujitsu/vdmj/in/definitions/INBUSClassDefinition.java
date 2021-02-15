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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.in.definitions;

import com.fujitsu.vdmj.in.definitions.visitors.INDefinitionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.values.BUSValue;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class INBUSClassDefinition extends INClassDefinition
{
	private static final long serialVersionUID = 1L;
	private static INBUSClassDefinition instance = null;

	public INBUSClassDefinition(TCNameToken className, TCClassType type, TCNameList supernames, INDefinitionList definitions)
	{
		super(className, type, supernames, definitions, new INDefinitionList(),
			new INDefinitionList(), new INClassList(), null, false);
		instance = this;
	}

	@Override
	public ObjectValue newInstance(INDefinition ctorDefinition, ValueList argvals, Context ctxt)
	{
		NameValuePairList nvpl = definitions.getNamedValues(ctxt);
		NameValuePairMap map = new NameValuePairMap();
		map.putAll(nvpl);

		return new BUSValue(classtype, map, argvals, this);
	}

	public static BUSValue makeVirtualBUS(ValueSet cpus)
	{
		return new BUSValue((TCClassType)instance.getType(), cpus, null);
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBUSClassDefinition(this, arg);
	}
}
