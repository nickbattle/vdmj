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

package com.fujitsu.vdmj.po.expressions;

import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POExplicitOperationDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitFunctionDefinition;
import com.fujitsu.vdmj.po.definitions.POImplicitOperationDefinition;
import com.fujitsu.vdmj.po.expressions.visitors.POExpressionVisitor;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class POVariableExpression extends POExpression
{
	private static final long serialVersionUID = 1L;

	public final TCNameToken name;
	public final PODefinition vardef;

	public POVariableExpression(TCNameToken name, PODefinition vardef)
	{
		super(name.getLocation());
		this.name = name;
		this.vardef = vardef;
	}

	@Override
	public String toString()
	{
		return name.getName() + (name.isOld() ? "~" : "");
	}

	@Override
	public String getPreName()
	{
		if (vardef instanceof POExplicitFunctionDefinition)
		{
			POExplicitFunctionDefinition ex = (POExplicitFunctionDefinition)vardef;

			if (ex.precondition == null)
			{
				return "";		// A function without a precondition
			}

			return ex.name.getPreName(location).getName();
		}
		else if (vardef instanceof POImplicitFunctionDefinition)
		{
			POImplicitFunctionDefinition im = (POImplicitFunctionDefinition)vardef;

			if (im.precondition == null)
			{
				return "";		// A function without a precondition
			}

			return im.name.getPreName(location).getName();
		}
		else if (vardef instanceof POExplicitOperationDefinition)
		{
			POExplicitOperationDefinition ex = (POExplicitOperationDefinition)vardef;

			if (ex.precondition == null)
			{
				return "";		// An operation without a precondition
			}

			return ex.name.getPreName(location).getName();
		}
		else if (vardef instanceof POImplicitOperationDefinition)
		{
			POImplicitOperationDefinition im = (POImplicitOperationDefinition)vardef;

			if (im.precondition == null)
			{
				return "";		// An operation without a precondition
			}

			return im.name.getPreName(location).getName();
		}

		return null;	// Not a function/operation.
	}

	@Override
	public <R, S> R apply(POExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseVariableExpression(this, arg);
	}
}
