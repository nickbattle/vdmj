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
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INNilExpression;
import com.fujitsu.vdmj.messages.RTValidator;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ValueException;
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
 *     not exists i2, t2 & O(e2, i2, t2)
 *         and t1 <= t2 and t2 < t1 + d
 *         and (m => i1 = i2)
 *         and (e1 = e2 => i2 = i1 + 1)
 * 
 * See http://dx.doi.org/10.1109/HASE.2007.26.
 */
public class INSeparateAnnotation extends INConjectureAnnotation
{
	private static final long serialVersionUID = 1L;
	
	private final String e1;
	private final INExpression condition;
	private final String e2;
	private final long delay;
	private final boolean match;

	public INSeparateAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
		
		this.e1 = args.get(0).toString();
		this.condition = args.get(1) instanceof INNilExpression ? null : args.get(1);
		this.e2 = args.get(2).toString();
		this.delay = Long.parseLong(args.get(3).toString());
		this.match = Boolean.parseBoolean(args.get(4).toString());
	}

	private static class Occurrence
	{
		public final long i1;
		public final long t1;
		public final long thid;
		
		public Occurrence(long i1, long t1, long thid)
		{
			this.i1 = i1;
			this.t1 = t1;
			this.thid = thid;
		}
	}
	
	private static class Failure
	{
		public final long t1;
		public final long thid1;
		public final long t2;
		public final long thid2;
		
		public Failure(long t1, long thid1, long t2, long thid2)
		{
			this.t1 = t1;
			this.thid1 = thid1;
			this.t2 = t2;
			this.thid2 = thid2;
		}
		
		@Override
		public String toString()
		{
			return t1 + ", " + thid1 + ", " + t2 + ", " + thid2;
		}
	}

	private final List<Occurrence> occurrences = new Vector<Occurrence>();
	private final List<Failure> failures = new Vector<Failure>();
	private long i1 = 0;
	private long i2 = 0;
	
	@Override
	public void processReset()
	{
		occurrences.clear();
		failures.clear();
		i1 = 0;
		i2 = 0;
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
				long time = Long.parseLong(record.get("time"));
				long thid = Long.parseLong(record.get("id"));

				if (event.equals(e1))
				{
					i1++;

					if (condition == null || condition.eval(ctxt).boolValue(ctxt))
					{
						occurrences.add(new Occurrence(i1, time, thid));
					}
				}
				
				if (event.equals(e2))
				{
					i2++;
					
					for (Occurrence occ: occurrences)
					{
						boolean T = occ.t1 <= time && time < occ.t1 + delay;	// t1 <= t2 < t1 + d
						boolean M = match ? occ.i1 == i2 : true;				// m => i1 = i2
						boolean E = e1.equals(e2) ? i2 == i1 + 1 : true;		// e1 = e2 => i2 = i1 + 1
						
						if (T && M && E)	// Not exists, so if all are true this is a failure
						{
							failures.add(new Failure(occ.t1, occ.thid, time, thid));
						}
					}
				}
			}
			catch (ValueException e)
			{
				System.err.println("Error in condition: " + e);
			}
			catch (NumberFormatException e)
			{
				System.err.println("Malformed record: " + e);
			}
		}
		
		return result;
	}

	@Override
	public void processComplete(File violations) throws IOException
	{
		for (Failure failure: failures)
		{
			System.err.println("FAIL: " + failure.toString());
		}
	}
}
