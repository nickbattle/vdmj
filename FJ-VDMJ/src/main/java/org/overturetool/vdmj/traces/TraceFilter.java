/*******************************************************************************
 *
 *	Copyright (C) 2015 Fujitsu Services Ltd.
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

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

/**
 * A class to filter whether tests are performed, by various criteria.
 */
public class TraceFilter
{
	private final int count;
	private final float subset;
	private final TraceReductionType reductionType;
	private final Random prng;
	
	private List<CallSequence> failedTests = new Vector<CallSequence>();
	private List<Integer> failedStems = new Vector<Integer>();
	private List<Integer> failedNumbers = new Vector<Integer>();
	private Set<String> shapes = new HashSet<String>();
	private int extras = 0;
	
	private Set<Integer> included = new HashSet<Integer>();
	
	public TraceFilter(int count, float subset, TraceReductionType reductionType, long seed)
	{
		this.count = count;
		this.subset = subset;
		this.reductionType = reductionType;
		this.prng = new Random(seed);
		
		// Generate explicit random tests to include for the subset, if there is one.
		if (subset < 1.0)
		{
    		for (int i=0; i<(count * subset); i++)
    		{
    			int n;
    			
    			do
    			{
    				n = prng.nextInt(count) + 1;
    			}
    			while (included.contains(n));
    			
    			included.add(n);
    		}
		}
	}

	public int getFilteredBy(CallSequence test)
	{
		for (int i=0; i<failedTests.size(); i++)
		{
			if (failedTests.get(i).compareStem(test, failedStems.get(i)))
			{
				return failedNumbers.get(i);
			}
		}
		
		return 0;
	}

	public void update(List<Object> result, CallSequence test, int n)
	{
		if (result.get(result.size()-1) != Verdict.PASSED)
		{
			failedTests.add(test);
			failedStems.add(result.size() - 1);
			failedNumbers.add(n);
		}
	}

	public boolean isRemoved(CallSequence test, int number)
	{
		switch (reductionType)
		{
			case NONE:
				return false;

			case RANDOM:
				if (included.size() > 0)
				{
					return !included.contains(number);
				}
				else
				{
					return false;	// 100% random
				}
				
			case SHAPES_NOVARS:
			case SHAPES_VARNAMES:
			case SHAPES_VARVALUES:
				String shape = test.toShape(reductionType);
				
				if (shapes.contains(shape))
				{
					if (included.size() > 0)
					{
						if (included.contains(number))
						{
							if (extras > 0)		// We already added extra tests
							{
								extras--;
								return true;	// So exclude this one
							}

							return false;
						}
						else
						{
							return true;
						}
					}
					else
					{
						return false;	// 100%
					}
				}
				else
				{
					shapes.add(shape);
					
					if (!included.contains(number))
					{
						extras++;
					}
					
					return false;	// Every shape appears once
				}
		}
		
		return false;
	}
	
	public int getFilteredCount()
	{
		if (subset < 1.0 && reductionType == TraceReductionType.RANDOM)
		{
			return (int)Math.ceil(count * subset);
		}
		else
		{
			// We don't know how many will be returned for shaped reductions
			return -1;
		}
	}
}
