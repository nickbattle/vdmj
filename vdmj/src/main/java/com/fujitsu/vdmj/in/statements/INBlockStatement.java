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

package com.fujitsu.vdmj.in.statements;

import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;

public class INBlockStatement extends INSimpleBlockStatement
{
	private static final long serialVersionUID = 1L;

	public final INDefinitionList assignmentDefs;

	public INBlockStatement(LexLocation location, INDefinitionList assignmentDefs, INStatementList stmts)
	{
		super(location, stmts);
		this.assignmentDefs = assignmentDefs;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("(\n");

		for (INDefinition d: assignmentDefs)
		{
			sb.append(d);
			sb.append("\n");
		}

		sb.append("\n");
		sb.append(super.toString());
		sb.append(")");
		return sb.toString();
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		Context evalContext = new Context(location, "block statement", ctxt);

		for (INDefinition d: assignmentDefs)
		{
			evalContext.putList(d.getNamedValues(evalContext));
		}

		return evalBlock(evalContext);
	}

	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseBlockStatement(this, arg);
	}
}
