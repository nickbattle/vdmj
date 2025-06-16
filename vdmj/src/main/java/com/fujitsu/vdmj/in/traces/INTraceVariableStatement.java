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

package com.fujitsu.vdmj.in.traces;

import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidValue;

public class INTraceVariableStatement extends INStatement
{
	private static final long serialVersionUID = 1L;
	public final INTraceVariable var;

	public INTraceVariableStatement(INTraceVariable var)
	{
		super(var.name.getLocation());
		this.var = var;
	}

	@Override
	public Value eval(Context ctxt)
	{
		location.hit();
		Value val = var.value;

		if (var.clone)
		{
			val = (Value)var.value.clone();		// To allow updates to objects
		}

		ctxt.put(var.name, val);
		return new VoidValue();
	}

	@Override
	public String toString()
	{
		return var.toString();
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		throw new RuntimeException("Cannot apply visitor to INTraceVariableStatement");
	}
}
