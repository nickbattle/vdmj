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

package com.fujitsu.vdmj.po.definitions;

import com.fujitsu.vdmj.po.definitions.visitors.PODefinitionVisitor;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to hold an external state definition.
 */
public class POExternalDefinition extends PODefinition
{
	private static final long serialVersionUID = 1L;
	public final PODefinition state;
	public final boolean readOnly;

	public POExternalDefinition(PODefinition state, boolean readOnly)
	{
		super(state.location, state.name);
		this.state = state;
		this.readOnly = readOnly;
	}

	@Override
	public String toString()
	{
		return (readOnly ? "ext rd " : "ext wr ") + state.name;
	}

	@Override
	public TCType getType()
	{
		return state.getType();
	}

	@Override
	public <R, S> R apply(PODefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExternalDefinition(this, arg);
	}
}
