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

import java.util.Vector;

/**
 * A list of trace iterators, and operations to perform on them as a group.
 */
public class TraceIteratorList extends Vector<TraceIterator>
{
	private static final long serialVersionUID = 1L;
	
	private CallSequence[] alternatives = null;

	private Integer lastAlternative = null;

	public void reset()
	{
		for (TraceIterator iter: this)
		{
			iter.reset();
		}
		
		alternatives = null;
		lastAlternative = null;
	}
	
	public int countSequence()
	{
		int result = 1;
		
		for (TraceIterator iter: this)
		{
			result = result * iter.count();
		}
		
		return result;
	}

	public int countAlternative()
	{
		int result = 0;
		
		for (TraceIterator iter: this)
		{
			result = result + iter.count();
		}
		
		return result;
	}

	public boolean hasMoreSequenceTests()
	{
		for (int i=0; i < size(); i++)
		{
			if (get(i).hasMoreTests())
			{
				return true;
			}
		}
		
		return false;
	}

	public boolean hasMoreAlternativeTests()
	{
		int i = (lastAlternative != null) ? lastAlternative : 0;
		
		for (; i<size(); i++)
		{
			if (get(i).hasMoreTests())
			{
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Add together one subsequence from each iterator.
	 */
	public CallSequence getNextTestSequence()
	{
		if (alternatives == null)	// First time in
		{
			alternatives  = new CallSequence[size()];
			
			for (int i=0; i<size(); i++)
			{
				alternatives[i] = get(i).getNextTest();
			}
		}
		else
		{
        	for (int i=0; i<size(); i++)
        	{
        		if (get(i).hasMoreTests())
        		{
        			alternatives[i] = get(i).getNextTest();
        			break;
        		}
        		else if (i < size() - 1) // && get(i+1).hasMoreTests())
        		{
        			get(i).reset();
        			alternatives[i] = get(i).getNextTest();
        		}
        	}
		}
		
		CallSequence seq = new CallSequence();
		
		for (int i=0; i<size(); i++)
		{
			seq.addAll(alternatives[i]);
		}
		
		return seq;
	}

	/**
	 * Return one subsequence from each iterator.
	 */
	public CallSequence getNextTestAlternative()
	{
		int i = (lastAlternative != null) ? lastAlternative : 0;
		
		for (; i<size(); i++)
		{
			if (get(i).hasMoreTests())
			{
				lastAlternative = i;
				return get(i).getNextTest();
			}
		}
		
		throw new RuntimeException("Called getNextTest() when !hasMoreTests()");
	}
	
	/**
	 * Get the simplest alternative iterator representing the list.
	 */
	public TraceIterator getAlternatveIterator()
	{
		if (size() == 1)
		{
			return get(0);
		}
		else
		{
			return new AlternativeIterator(this);
		}
	}
	
	/**
	 * Get the simplest sequence iterator representing the list.
	 */
	public TraceIterator getSequenceIterator()
	{
		if (size() == 1)
		{
			return get(0);
		}
		else
		{
			return new SequenceIterator(this);
		}
	}
}
