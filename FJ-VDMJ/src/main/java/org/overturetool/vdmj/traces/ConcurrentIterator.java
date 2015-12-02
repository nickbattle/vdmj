/*******************************************************************************
 *
 *	Copyright (c) 2010 Fujitsu Services Ltd.
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

public class ConcurrentIterator extends TraceIterator
{
	private final TraceIteratorList nodes;
	
	private PermuteArray permutations = null;
	
	private Permutor permutor = null;

	public ConcurrentIterator(TraceIteratorList nodes)
	{
		this.nodes = nodes;
	}

	@Override
	public CallSequence getNextTest()
	{
		if (permutations == null)
		{
			permutations = new PermuteArray(nodes.size());
			permutor = null;
		}
		
		if (permutor == null)
		{
			int[] c = new int[nodes.size()];
			
			for (int i=0; i<nodes.size(); i++)
			{
				c[i] = nodes.get(i).count();
			}
			
			permutor = new Permutor(c);
		}

		CallSequence result = new CallSequence();
		
		if (permutor.hasNext())
		{
			int[] select = permutor.next();
			int[] permutation = permutations.next();
			
			// The select array contains a set of numbers, being the elements of
			// the expansion of the permutation that must be concatenated.
			
			CallSequence[] subsequences = new CallSequence[nodes.size()];
			nodes.reset();
			
			for (int node=0; node<nodes.size(); node++)
			{
//				for (int i=0; i<repeatCount; i++)
//				{
//					subsequences[i] = repeat.getNextTest();
//				}
//				
//				for (int i=0; i<repeatValue; i++)	// ie. {n} additions
//				{
//					result.addAll(subsequences[select[i]]);
//				}
			}
		}

		if (!permutor.hasNext())		// Exhausted permutor for repeatValue
		{
			permutor = null;

//			if (repeatValue < to)
//			{
//				repeatValue++;
//			}
//			else
//			{
//				repeatValue = null;		// Indicates no more tests
//			}
		}
		
		return result;
		
		
//		List<TestSequence> nodetests = new Vector<TestSequence>();
//		int count = nodes.size();
//
//		for (TraceNode node: nodes)
//		{
//			nodetests.add(node.getTests());
//		}
//
//		TestSequence tests = new TestSequence();
//		PermuteArray pa = new PermuteArray(count);
//
//		while (pa.hasNext())
//		{
//			int[] sizes = new int[count];
//			int[] perm = pa.next();
//
//			for (int i=0; i<count; i++)
//			{
//				sizes[i] = nodetests.get(perm[i]).size();
//			}
//
//			Permutor p = new Permutor(sizes);
//
//    		while (p.hasNext())
//    		{
//    			int[] select = p.next();
//    			CallSequence seq = getVariables();
//
//    			for (int i=0; i<count; i++)
//    			{
//    				TestSequence ith = nodetests.get(perm[i]);
//    				
//    				if (!ith.isEmpty())
//    				{
//    					CallSequence subseq = ith.get(select[i]);
//    					seq.addAll(subseq);
//    				}
//    			}
//
//    			tests.add(seq);
//    		}
//		}
//
//		return tests;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("|| (");
		String sep = "";

		for (TraceIterator node: nodes)
		{
			sb.append(sep);
			sb.append(node.toString());
			sep = ", ";
		}

		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean hasMoreTests()
	{
		return nodes.hasMoreTests();
	}

	@Override
	public int count()
	{
		return nodes.countAlternative();
	}

	@Override
	public void reset()
	{
		nodes.reset();
	}
}
