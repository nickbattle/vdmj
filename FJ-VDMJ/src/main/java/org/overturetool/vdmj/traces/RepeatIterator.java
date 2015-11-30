/*******************************************************************************
 *
 *	Copyright (C) 2008, 2009 Fujitsu Services Ltd.
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
 *
 ******************************************************************************/

package org.overturetool.vdmj.traces;

public class RepeatIterator extends TraceIterator
{
	public final TraceIterator repeat;
	public final int from;
	public final int to;

	public RepeatIterator(TraceIterator repeat, long from, long to)
	{
		this.repeat = repeat;
		this.from = (int)from;
		this.to = (int)to;
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
//		TestSequence tests = new TestSequence();
//		TestSequence rtests = repeat.getNextTest();
//		int count = rtests.size();
//
//		for (int r = from; r <= to; r++)
//		{
//			if (r == 0)
//			{
//				CallSequence seq = getVariables();
//   				seq.add(new SkipStatement(new LexLocation()));
//    			tests.add(seq);
//				continue;
//			}
//
// 			int[] c = new int[r];
//
//			for (int i=0; i<r; i++)
//			{
//				c[i] = count;
//			}
//
//			Permutor p = new Permutor(c);
//
//			while (p.hasNext())
//			{
//	   			CallSequence seq = getVariables();
//	   			int[] select = p.next();
//
//	   			for (int i=0; i<r; i++)
//    			{
//    				seq.addAll(rtests.get(select[i]));
//    			}
//
//    			tests.add(seq);
//			}
//		}
//
//		return tests;
		
		return new CallSequence();
	}

	@Override
	public boolean hasMoreTests()
	{
		return repeat.hasMoreTests();
	}

	@Override
	public int count()
	{
		return to - from + 1;
	}

	@Override
	public void reset()
	{
		repeat.reset();
	}

	@Override
	public boolean isReset()
	{
		return repeat.isReset();
	}
}
