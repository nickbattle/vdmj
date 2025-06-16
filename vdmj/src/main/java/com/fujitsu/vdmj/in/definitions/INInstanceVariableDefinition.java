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
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * A class to represent instance variable definitions.
 */
public class INInstanceVariableDefinition extends INAssignmentDefinition
{
	private static final long serialVersionUID = 1L;
	public final TCNameToken oldname;

	public INInstanceVariableDefinition(INAccessSpecifier accessSpecifier,
			TCNameToken name, TCType type, INExpression expression, TCType expType)
	{
		super(accessSpecifier, name, type, expression, expType);
		oldname = name.getOldName();
	}

	@Override
	public boolean isInstanceVariable()
	{
		return true;
	}

	@Override
	public <R, S> R apply(INDefinitionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseInstanceVariableDefinition(this, arg);
	}
}
