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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;

import com.fujitsu.vdmj.in.expressions.INExpressionList;
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
 * The required separation conjecture is similar to the separation conjecture but additionally
 * requires that the e2 event does occur. A conjecture SepRequire(e1, c, e2, d, m) evaluates
 * to true over an execution trace if and only if:
 * 
 * forall i1, t1 & O(e1, i1, t1) and E(c, t1) =>
 *     not exists i2, t2 & O(e2, i2, t2)
 *         and t1 <= t2 and t2 < t1 + d
 *         and (m => i1 = i2)
 *         and (e1 = e2 => i2 = i1 + 1)
 *     and exists i3, t3 & O(e2 , i3 , t3)
 *         and (m => i1 = i3 )
 *         and (e1 = e2 => i3 = i1 + 1)
 * 
 * See http://dx.doi.org/10.1109/HASE.2007.26.
 */
public class INSepRequireAnnotation extends INConjectureAnnotation
{
	private static final long serialVersionUID = 1L;

	public INSepRequireAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
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
					
					Iterator<Occurrence> iter = occurrences.iterator();
					
					while (iter.hasNext())
					{
						Occurrence occ = iter.next();

						boolean T = occ.t1 <= time && time < occ.t1 + delay;	// t1 <= t2 < t1 + d
						boolean M = match ? occ.i1 == i2 : true;				// m => i1 = i2
						boolean E = e1.equals(e2) ? i2 == i1 + 1 : true;		// e1 = e2 => i2 = i1 + 1
						
						if (T && M && E)	// Not exists, so if all are true this is a failure
						{
							failures.add(new Failure(occ.t1, occ.thid, time, thid));
							iter.remove();
							result = false;
						}
						else if (!T && M && E)	// Too late
						{
							failures.add(new Failure(occ.t1, occ.thid, time, thid));
							iter.remove();
							result = false;
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
	public int processComplete(File violations) throws IOException
	{
		// If any occurrences remain, they have never been satisfied, so error.
		
		for (Occurrence occ: occurrences)
		{
			failures.add(new Failure(occ.t1, occ.thid));
		}
		
		PrintWriter pw = new PrintWriter(new FileWriter(violations, true));
		
		try
		{
			for (Failure failure: failures)
			{
				pw.println(failure.toString());
			}
		}
		finally
		{
			pw.close();
		}
		
		return failures.size();
	}
}
