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

	protected final String name;
	protected final String e1;
	protected final INExpression condition;
	protected final String e2;
	protected final long delay;
	protected final boolean match;

	public INConjectureAnnotation(TCIdentifierToken name, INExpressionList args)
	{
		super(name, args);
		
		this.name = "C" + (++counter);
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
		public final INConjectureAnnotation conj;
		public final long t1;
		public final long thid1;
		public final long t2;
		public final long thid2;
		
		public Failure(INConjectureAnnotation annotation, long t1, long thid1, long t2, long thid2)
		{
			this.conj = annotation;
			this.t1 = t1;
			this.thid1 = thid1;
			this.t2 = t2;
			this.thid2 = thid2;
		}
		
		public Failure(INConjectureAnnotation annotation, long t1, long thid1)
		{
			this.conj = annotation;
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
				return String.format("\"%s\" \"%s, %s, %s, %d\" %d %d",
						conj.name, conj.e1, conj.condition, conj.e2, conj.delay, t1, thid1);
			}
			else
			{
				return String.format("\"%s\" \"%s, %s, %s, %d\" %d %d %d %d",
						conj.name, conj.e1, conj.condition, conj.e2, conj.delay, t1, thid1, t2, thid2);
			}
		}
	}

	protected final List<Occurrence> occurrences = new Vector<Occurrence>();
	protected final List<Failure> failures = new Vector<Failure>();
	protected long i1 = 0;
	protected long i2 = 0;
	
	private static int counter = 0;		// Names conjectures, C1, C2, etc

	@Override
	public void processReset()
	{
		occurrences.clear();
		failures.clear();
		i1 = 0;
		i2 = 0;
	}

	public static void doInit()
	{
		counter = 0;
	}
	
	@Override
	public int processComplete(File violations) throws IOException
	{
		PrintWriter pw = new PrintWriter(new FileWriter(violations, true));
		
		try
		{
			if (failures.isEmpty())
			{
				pw.printf("\"%s\" \"%s, %s, %s, %d\" PASS\n", name, e1, condition, e2, delay);
			}
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
