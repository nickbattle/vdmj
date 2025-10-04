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

import java.math.BigInteger;

import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.statements.INSimpleBlockStatement;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.in.statements.INWhileStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.Value;

public class INLoopMeasureAnnotation extends INAnnotation
{
	private static final long serialVersionUID = 1L;

	private final INStatement stmt;
	private final TCNameToken measureName;

	public INLoopMeasureAnnotation(TCIdentifierToken name, INExpressionList args,
		INStatement stmt, TCNameToken measureName)
	{
		super(name, args);
		this.stmt = stmt;
		this.measureName = measureName;

		setBreaks(false);	// Break on checks instead
	}

	public void before(Context ctxt)
	{
		Value startValue = args.firstElement().eval(ctxt).deref();
		ctxt.put(measureName, startValue);
	}

	public void check(Context ctxt) throws ValueException
	{
		String message = null;
		LexLocation at = null;

		try
		{
			Value currentValue = args.firstElement().eval(ctxt).deref();	// UpdatableValue
			BigInteger current = currentValue.natValue(ctxt);

			Value previousValue = ctxt.check(measureName);
			BigInteger previous = previousValue.natValue(ctxt);

			if (previous.compareTo(current) > 0)
			{
				ctxt.put(measureName, currentValue);	// Fine
				return;
			}
			else
			{
				message = "Loop measure failed: prev " + previous + ", curr " + current;
				at = lastLocation();
			}
		}
		catch (Exception e)
		{
			message = "Loop measure failed: " + e.getMessage();
			at = location;
		}

		throw new ContextException(4179,	message, at, ctxt);
	}

	private LexLocation lastLocation()
	{
		if (stmt instanceof INWhileStatement)
		{
			INWhileStatement wstmt = (INWhileStatement)stmt;
			
			if (wstmt.statement instanceof INSimpleBlockStatement)
			{
				INSimpleBlockStatement block = (INSimpleBlockStatement)wstmt.statement;
				return block.statements.lastElement().location;
			}
			else
			{
				return wstmt.statement.location;
			}
		}
		else
		{
			return location;
		}
	}

	public void removeMeasure(Context ctxt)
	{
		ctxt.remove(measureName);
	}
}
