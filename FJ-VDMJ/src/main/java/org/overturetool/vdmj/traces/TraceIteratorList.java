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

import java.util.Vector;

/**
 * A list of trace iterators.
 */
public class TraceIteratorList extends Vector<TraceIterator>
{
	private static final long serialVersionUID = 1L;

	public void reset()
	{
		for (TraceIterator iter: this)
		{
			iter.reset();
		}
	}
	
	public boolean isReset()
	{
		for (TraceIterator iter: this)
		{
			if (!iter.isReset())
			{
				return false;
			}
		}
		
		return true;
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

	public boolean hasMoreTests()
	{
		for (TraceIterator iter: this)
		{
			if (iter.hasMoreTests())
			{
				return true;
			}
		}
		
		return false;
	}
	
	public CallSequence getLastTest()
	{
		CallSequence seq = new CallSequence();
		
		for (TraceIterator iter: this)
		{
			seq.addAll(iter.getLastTest());
		}
		
		return seq;
	}

	/**
	 * Add together one subsequence from each iterator.
	 */
	public CallSequence getNextTestSequence()
	{
		for (int i=0; i<size(); i++)
		{
			if (get(i).hasMoreTests())
			{
    			get(i).getNextTest();	// Sets lastTest for this iterator
				break;
			}

			get(i).reset();
		}
		
		CallSequence seq = new CallSequence();
		
		for (TraceIterator iter: this)
		{
			if (iter.getLastTest() == null)
			{
				seq.addAll(iter.getNextTest());
			}
			else
			{
				seq.addAll(iter.getLastTest());
			}
		}
		
		return seq;
	}

	/**
	 * Return one subsequence from each iterator.
	 */
	public CallSequence getNextTestAlternative()
	{
		for (int i=0; i<size(); i++)
		{
			if (get(i).hasMoreTests())
			{
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
