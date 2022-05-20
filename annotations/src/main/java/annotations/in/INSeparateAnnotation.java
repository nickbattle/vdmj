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
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

/**
 * We will write O(e, i , t) to indicate that the i th occurrence of event e takes place at time t.
 * The variable i ranges over the non-zero natural numbers N1, and t, representing time, ranges
 * over the indices of the trace.
 * 
 * We will write E(p, t) to mean that the state predicate p is true of the variables in the
 * execution trace at time t.
 * 
 * A separation conjecture is a 5-tuple Separate(e1, c, e2, d , m) where e1 and e2 are the names of
 * events, c is a state predicate, d is the minimum acceptable delay between an occurrence of e1 and
 * any following occurrence of e2 provided that c evaluates to true at the occurrence time of e1.
 * If c evaluates to false when e1 occurs, the validation conjecture holds independently of the
 * occurrence time of e2. The Boolean flag m is called the ‘match flag’, when set to true, indicates
 * a requirement that the occurrence numbers of e1 and e2 should be equal.
 * 
 * A validation conjecture Separate(e1, c, e2, d , m) evaluates true over an execution trace if
 * and only if:
 * 
 * forall i1, t1 & O(e1, i1, t1) and E(c, t1) =>
 *     not exists i2, t2 & O(e2, i2, t2) and t1 <= t2 < t1 + d and
 *     (m => i1 = i2) and (e1 = e2 => i2 = i1 + 1)
 * 
 * See http://dx.doi.org/10.1109/HASE.2007.26.
 */
public class INSeparateAnnotation extends INConjectureAnnotation
{
	private static final long serialVersionUID = 1L;

	public INSeparateAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
	}

	@Override
	public void processReset()
	{
	}

	@Override
	public boolean process(Map<String, String> record)
	{
		return true;
	}

	@Override
	public void processComplete(File violations) throws IOException
	{
	}
}
