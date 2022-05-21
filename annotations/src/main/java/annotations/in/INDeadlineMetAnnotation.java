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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package annotations.in;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

/**
 * We will write O(e, i , t) to indicate that the i th occurrence of event e takes place at time t.
 * The variable i ranges over the non-zero natural numbers N1, and t, representing time, ranges
 * over the indices of the trace.
 * 
 * We will write E(p, t) to mean that the state predicate p is true of the variables in the
 * execution trace at time t.
 * 
 * A Deadline conjecture places a maximum delay on the occurrence of the reaction event. Again,
 * the match option may be used to link the occurrence numbers of the stimulus and reaction events.
 * 
 * A validation conjecture DeadlineMet(e1, c, e2, d , m) consists of a stimulus event, condition
 * and reaction event; if c holds, d is the maximum tolerable delay between stimulus and reaction.
 * The conjecture evaluates true over an execution trace if and only if:
 * 
 * forall i1, t1 & O(e1, i1, t1) and E(c, t1) =>
 *    exists i2, t2 & O(e2, i2, t2) and t1 <= t2 <= t1 + d and
 *    (m => i1 = i2) and (e1 = e2 => i2 = i1 + 1)
 * 
 * See http://dx.doi.org/10.1109/HASE.2007.26.
 */
public class INDeadlineMetAnnotation extends INConjectureAnnotation
{
	private static final long serialVersionUID = 1L;

	public INDeadlineMetAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void processReset()
	{
	}

	@Override
	public boolean process(Map<String, String> record, Context ctxt)
	{
		return true;
	}

	@Override
	public void processComplete(File violations) throws IOException
	{
	}
}
