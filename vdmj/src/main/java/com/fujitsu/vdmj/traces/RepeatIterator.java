/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.traces;

import java.util.Arrays;

import com.fujitsu.vdmj.in.statements.INSkipStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.util.Selector;

public class RepeatIterator extends TraceIterator
{
	private static final INSkipStatement SKIP = new INSkipStatement(LexLocation.ANY);
	private final TraceIterator repeat;
	private final int from;
	private final int to;
	
	private int repeatCount;
	private Integer repeatValue;
	private Selector permutor;
	
	public RepeatIterator(TraceIterator repeat, long from, long to)
	{
		this.repeat = repeat;
		this.from = (int)from;
		this.to = (int)to;
		
		repeatValue = this.from;
		repeatCount = repeat.count();
	}

	@Override
	public String toString()
	{
		return repeat.toString() +
    		((from == 1 && to == 1) ? "" :
    			(from == to) ? ("{" + from + "}") :
    				("{" + from + ", " + to + "}"));
	}

	@Override
	public CallSequence getNextTest()
	{
		if (permutor == null)	// Start new permutor for repeatValue
		{
			int[] c = new int[repeatValue];
			Arrays.fill(c, repeatCount);
			permutor = new Selector(c);
		}
		
		CallSequence test = getVariables();
		
		if (permutor.hasNext())
		{
			int[] select = permutor.next();
			
			// The select array contains a set of numbers, being the elements of
			// the expansion of "repeat" that must be concatenated.
			
			CallSequence[] subsequences = new CallSequence[repeatCount];
			repeat.reset();
			
			for (int i=0; i<repeatCount; i++)
			{
				subsequences[i] = repeat.getNextTest();
			}
			
			for (int i=0; i<repeatValue; i++)	// ie. {n} additions
			{
				test.addAll(subsequences[select[i]]);
			}
		}

		if (!permutor.hasNext())		// Exhausted permutor for repeatValue
		{
			permutor = null;

			if (repeatValue == 0)		// ie. "?" or zero times
			{
				test.add(SKIP);
			}
			
			if (repeatValue < to)
			{
				repeatValue++;
			}
			else
			{
				repeatValue = null;		// Indicates no more tests
			}
		}
		
		return test;
	}

	@Override
	public boolean hasMoreTests()
	{
		return repeatValue != null;		// Cleared by getNextTest
	}

	@Override
	public int count()
	{
		int n = 0;
		
		for (int rval=from; rval <= to; rval++)
		{
			n = (int) (n + Math.pow(repeatCount, rval));
		}
		
		return n;
	}

	@Override
	public void reset()
	{
		repeat.reset();
		repeatValue = from;
		permutor = null;
	}
}
