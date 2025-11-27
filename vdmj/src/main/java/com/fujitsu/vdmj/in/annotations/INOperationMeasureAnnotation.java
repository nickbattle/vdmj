/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package com.fujitsu.vdmj.in.annotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.values.Value;

public class INOperationMeasureAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;

	private final Map<Long, Stack<Value>> measureValues;

	public INOperationMeasureAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
		measureValues = new HashMap<Long, Stack<Value>>();
		setBreaks(false);
	}

	@Override
	protected void doInit(Context ctxt)
	{
		measureValues.clear();
	}

	public void called(Context ctxt) throws ValueException
	{
		long tid = Thread.currentThread().getId();
		Stack<Value> stack = measureValues.get(tid);

		Value currentMeasure = args.firstElement().eval(ctxt).deref();	// UpdatableValue

		if (stack == null)
		{
			stack = new Stack<Value>();
			measureValues.put(tid, stack);
		}

		if (!stack.isEmpty())	// Not the first call
		{
			Value lastMeasure = stack.peek();

			if (lastMeasure != null && currentMeasure.compareTo(lastMeasure) >= 0)
			{
				String message = "Measure failure: current " + currentMeasure + ", previous " + lastMeasure;
				measureValues.clear();	// Re-initialise measure counters etc. for failure
				throw new ValueException(4146, message, ctxt);
			}
		}

		stack.push(currentMeasure);
	}

	public void returned()
	{
		long tid = Thread.currentThread().getId();
		measureValues.get(tid).pop();
	}
}
