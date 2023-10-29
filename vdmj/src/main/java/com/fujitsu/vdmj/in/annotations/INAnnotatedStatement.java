/*******************************************************************************
 *
 *	Copyright (c) 2018 Nick Battle.
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

package com.fujitsu.vdmj.in.annotations;

import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.visitors.INStatementVisitor;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.values.Value;

public class INAnnotatedStatement extends INStatement
{
	private static final long serialVersionUID = 1L;

	public final INAnnotation annotation;
	public final INStatement statement;
	
	public INAnnotatedStatement(LexLocation location, INAnnotation annotation, INStatement statement)
	{
		super(location);
		this.annotation = (annotation != null) ? annotation : new INNoAnnotation();
		this.statement = statement;
	}

	@Override
	public String toString()
	{
		return annotation + " " + statement;
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);
		if (!INAnnotation.suspended) annotation.inBefore(this, ctxt);
		Value rv = statement.eval(ctxt);
		if (!INAnnotation.suspended) annotation.inAfter(this, rv, ctxt);
		return rv;
	}
	
	@Override
	public <R, S> R apply(INStatementVisitor<R, S> visitor, S arg)
	{
		return visitor.caseAnnotatedStatement(this, arg);
	}
}
