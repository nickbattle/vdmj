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

package com.fujitsu.vdmj.po.statements;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POVariableExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class POIdentifierDesignator extends POStateDesignator
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken name;
	public final PODefinition vardef;

	public POIdentifierDesignator(TCNameToken name, PODefinition vardef)
	{
		super(name.getLocation());
		this.name = name;
		this.vardef = vardef;
	}

	@Override
	public String toString()
	{
		return name.getName();
	}
	
	@Override
	public POExpression toExpression()
	{
		return new POVariableExpression(name, vardef);
	}
}
