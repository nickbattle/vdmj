/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package vdmj;

import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.traces.TraceIterator;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.Value;

public class INGenerateExpression extends INExpression
{
	private static final long serialVersionUID = 1L;
	private final INNamedTraceDefinition tracedef;
	
	public INGenerateExpression(INNamedTraceDefinition tracedef)
	{
		super(tracedef.location);	// Location of the trace to generate
		this.tracedef = tracedef;
	}

	@Override
	public String toString()
	{
		return "generate " + tracedef;
	}

	@Override
	public Value eval(Context ctxt)
	{
		try
		{
			INClassDefinition traceClassDef = tracedef.classDefinition;
			Context traceContext = Interpreter.getInstance().getTraceContext(traceClassDef);
			TraceIterator traceIterator = tracedef.getIterator(traceContext);
			return new IntegerValue(traceIterator.count());
		}
		catch (ValueException e)
		{
			abort(e);
			return null;
		}
		catch (ContextException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new ContextException(0, e.getMessage(), location, ctxt);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseExpression(this, arg);	// Default
	}
}
