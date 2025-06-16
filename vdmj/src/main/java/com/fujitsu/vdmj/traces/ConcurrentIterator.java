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

import com.fujitsu.vdmj.util.Selector;


public class ConcurrentIterator extends TraceIterator
{
	private final TraceIteratorList nodes;
	
	private PermuteArray permutations = null;	// Of the node orderings
	private int[] permutation = null;			// The current ordering
	private Selector selector = null;			// Of the alternatives from each node

	public ConcurrentIterator(TraceIteratorList nodes)
	{
		this.nodes = nodes;
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
	public CallSequence getNextTest()
	{
		int nodesSize = nodes.size();
		
		if (permutations == null)	// First time in
		{
			permutations = new PermuteArray(nodes.size());
			selector = null;
		}
		
		if ((selector == null || !selector.hasNext()) &&	// Start next perm
			permutations.hasNext())
		{
			permutation = permutations.next();
			selector = null;
		}
		
		if (selector == null)		// First for a given permutation
		{
			int[] c = new int[nodesSize];
			
			for (int i=0; i<nodesSize; i++)
			{
				c[i] = nodes.get(i).count();
			}
			
			selector = new Selector(c);
		}

		CallSequence test = getVariables();
		
		if (selector.hasNext())
		{
			int[] select = selector.next();
			
			// The select array contains a set of numbers, being the elements of
			// the expansion of each node that must be concatenated.
			
			CallSequence[] subsequences = new CallSequence[nodesSize];
			nodes.reset();
			
			for (int node=0; node<nodesSize; node++)
			{
				for (int i=0; i<=select[node]; i++)		// Drive each iterator to the nth
				{
					subsequences[node] = nodes.get(node).getNextTest();
				}
			}
			
			for (int i=0; i<nodesSize; i++)		// Add in permutation order
			{
				test.addAll(subsequences[permutation[i]]);
			}
		}
		
		return test;
	}

	@Override
	public boolean hasMoreTests()
	{
		return permutations == null || permutations.hasNext() ||
			(selector != null && selector.hasNext());
	}

	@Override
	public int count()
	{
		return nodes.countSequence() * factorial(nodes.size());
	}

	private int factorial(int size)
	{
		return (size == 1) ? 1 : size * factorial(size - 1);
	}

	@Override
	public void reset()
	{
		nodes.reset();
		permutations = null;
	}
}
