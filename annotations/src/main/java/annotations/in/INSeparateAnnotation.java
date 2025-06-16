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

package annotations.in;

import java.util.Iterator;
import java.util.Map;

import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.messages.RTValidator;
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
 * forall i1, t1 &amp; O(e1, i1, t1) and E(c, t1) =&gt;
 *     not exists i2, t2 &amp; O(e2, i2, t2)
 *         and t1 &lt;= t2 and t2 &lt; t1 + d
 *         and (m =&gt; i1 = i2)
 *         and (e1 = e2 =&gt; i2 = i1 + 1)
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
	
	public static void doInit()
	{
		INConjectureAnnotation.doInit();
	}

	@Override
	public boolean process(Map<String, String> record, Context ctxt)
	{
		String event = record.get(RTValidator.HISTORY);	// eg. "#req(A`op)" or "#fin(Z`op)"
		boolean result = true;
		
		if (event != null)
		{
			try
			{
				if (event.equals(e1))
				{
					i1++;

					if (checkCondition(ctxt))
					{
						occurrences.add(new Occurrence(i1, record));
					}
				}
				
				if (event.equals(e2))
				{
					if (e1.equals(e2))
					{
						i2 = i1;	// Already counted above
					}
					else
					{
						i2++;
					}
					
					long time = Long.parseLong(record.get("time"));
					Iterator<Occurrence> iter = occurrences.iterator();
					
					while (iter.hasNext())
					{
						Occurrence occ = iter.next();
						
						boolean T = occ.time() <= time && time < occ.time() + delay;	// t1 <= t2 < t1 + d
						boolean M = match ? occ.i1 == i2 : true;						// m => i1 = i2
						boolean E = e1.equals(e2) ? i2 == occ.i1 + 1 : true;			// e1 = e2 => i2 = i1 + 1
						
						if (T && M && E)	// Not exists, so if all are true this is a failure
						{
							failures.add(new Failure(this, occ, new Occurrence(i2, record)));
							iter.remove();
							result = false;
						}
						else if (!T && M && E)	// Success, so remove occurrence
						{
							iter.remove();
						}
					}
				}
			}
			catch (NumberFormatException e)
			{
				System.err.println("Malformed record: " + e);
			}
		}
		
		return result;
	}
}
