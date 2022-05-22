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
import java.util.Vector;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INNilExpression;
import com.fujitsu.vdmj.messages.ConjectureProcessor;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;

public abstract class INConjectureAnnotation extends INAnnotation implements ConjectureProcessor
{
	private static final long serialVersionUID = 1L;

	protected final String e1;
	protected final INExpression condition;
	protected final String e2;
	protected final long delay;
	protected final boolean match;

	public INConjectureAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
		
		this.e1 = args.get(0).toString();
		this.condition = args.get(1) instanceof INNilExpression ? null : args.get(1);
		this.e2 = args.get(2).toString();
		this.delay = Long.parseLong(args.get(3).toString());
		this.match = Boolean.parseBoolean(args.get(4).toString());
	}

	protected static class Occurrence
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
	
	protected static class Failure
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
		
		public Failure(long t1, long thid1)
		{
			this.t1 = t1;
			this.thid1 = thid1;
			this.t2 = -1;
			this.thid2 = -1;
		}
		
		@Override
		public String toString()
		{
			if (t2 < 0)
			{
				return "FAIL: " + t1 + ", " + thid1;
			}
			else
			{
				return "FAIL: " + t1 + ", " + thid1 + ", " + t2 + ", " + thid2;
			}
		}
	}

	protected final List<Occurrence> occurrences = new Vector<Occurrence>();
	protected final List<Failure> failures = new Vector<Failure>();
	protected long i1 = 0;
	protected long i2 = 0;
	
	@Override
	public void processReset()
	{
		occurrences.clear();
		failures.clear();
		i1 = 0;
		i2 = 0;
	}

	@Override
	public int processComplete(File violations) throws IOException
	{
		for (Failure failure: failures)
		{
			System.out.println(failure.toString());
		}
		
		return failures.size();
	}
}
