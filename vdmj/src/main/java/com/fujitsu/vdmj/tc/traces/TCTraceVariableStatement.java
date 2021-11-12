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

package com.fujitsu.vdmj.tc.traces;

import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.statements.TCStatement;
import com.fujitsu.vdmj.tc.statements.visitors.TCStatementVisitor;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;

public class TCTraceVariableStatement extends TCStatement
{
	private static final long serialVersionUID = 1L;
	public final TCTraceVariable var;

	public TCTraceVariableStatement(TCTraceVariable var)
	{
		super(var.name.getLocation());
		this.var = var;
	}

	@Override
	public String toString()
	{
		return var.toString();
	}

	@Override
	public TCType typeCheck(Environment env, NameScope scope, TCType constraint, boolean mandatory)
	{
		FlatEnvironment flat = (FlatEnvironment)env;
		flat.add(new TCLocalDefinition(location, var.name, var.type));
		return setType(var.type);
	}

	@Override
	public <R, S> R apply(TCStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseTraceVariableStatement(this, arg);
	}
}
