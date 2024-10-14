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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.values;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.traces.PermuteArray;
import com.fujitsu.vdmj.util.KCombinator;
import com.fujitsu.vdmj.util.Utils;

/**
 * A set of values. Note that although this class implements a set (no duplicates)
 * it is not based on a java.util.Set<Value>, but rather a Vector<Value>. This is
 * so that the possible orderings of set values can be enumerated when
 * performing quantifiers like "a,b,c in set {{1,2,3}, {4,5,6}}".
 */

@SuppressWarnings("serial")
public class ValueSet extends Vector<Value>		// NB based on Vector
{
	private boolean isSorted;

	public ValueSet()
	{
		super();
		isSorted = true;
	}

	public ValueSet(int size)
	{
		super(size);
		isSorted = true;
	}

	public ValueSet(ValueSet from)
	{
		super(from.size());
		addAll(from);
		isSorted = from.isSorted;
	}

	public ValueSet(Value v)
	{
		add(v);
		isSorted = true;
	}

	public ValueSet(Value ...values)
	{
		super(values.length);
		
		for (Value v: values)
		{
			add(v);
		}
	}
	
	public boolean isSorted()
	{
		return isSorted;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other instanceof ValueSet)
		{
			ValueSet os = (ValueSet)other;
			
			if (os.size() != size())
			{
				return false;
			}
			else
			{
				return os.containsAll(this);	// Lookup for every element...
			}
		}

		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 0;

		for (Value v: this)
		{
			hash += v.hashCode();
		}

		return hash;
	}

	@Override
	public boolean add(Value v)
	{
		if (contains(v))
		{
			return true;
		}
		else
		{
			isSorted = false;
			return super.add(v);
		}
	}
	
	/**
	 * Add an item, given we know it is already sorted after the add.
	 * This is used by powersets, for efficiency.
	 */
	public boolean addNoSort(Value v)
	{
		return super.add(v);
	}

	public boolean addNoCheck(Value v)
	{
		isSorted = false;
		return super.add(v);	// Used by power set function
	}

	@Override
	public boolean addAll(Collection<? extends Value> values)
	{
		for (Value v: values)
		{
			add(v);
		}

		return true;
	}

	@Override
	public String toString()
	{
		return Utils.listToString("{", this, ", ", "}");
	}

	public void sort()
	{
		if (!isSorted)
		{
			Collections.sort(this);
			isSorted = true;
		}
	}

	public List<ValueSet> permutedSets()
	{
		// This is a 1st order permutation, which does not take account of the possible
		// nesting of sets or the presence of other permutable values with them (maps).

		List<ValueSet> results = new Vector<ValueSet>();
		int size = size();

		if (size == 0)
		{
			results.add(new ValueSet());	// Just {}
		}
		else
		{
    		PermuteArray p = new PermuteArray(size);

    		while (p.hasNext())
    		{
    			ValueSet m = new ValueSet(size);
    			int[] perm = p.next();

    			for (int i=0; i<size; i++)
    			{
    				m.add(get(perm[i]));
    			}

    			results.add(m);
    		}
		}

		return results;
	}

	public List<ValueSet> powerSet(Breakpoint breakpoint, Context ctxt)
	{
   		if (size() > Properties.in_powerset_limit)
		{
			throw new InternalException(0073, "Cannot evaluate power set of size " + size());
		}
   		
   		// The generation below depends on a sorted set to start with. This is
   		// normally the case, which doesn't cost us, so we sort here anyway.
   		sort();
   		
		List<ValueSet> sets = new Vector<ValueSet>(2^size());

		if (isEmpty())
		{
			sets.add(new ValueSet());	// Just {}
		}
		else
		{
			/**
			 * The KCombinator below produces combinations in order (eg. [1,2] before [1,3]).
			 * And we loop the combination sizes from large to small, which is also the
			 * natural ordering for sets. This means we can use addNoSort and (in power itself)
			 * we can construct the final SetValue without sorting, which is much more efficient.
			 */
			int size = size();
			long check = 0;
			
			for (int ss=size; ss>0; ss--)
			{
				for (int[] kc: new KCombinator(size, ss))
				{
					ValueSet ns = new ValueSet(ss);

					for (int i=0; i<ss; i++)
					{
						ns.addNoSort(get(kc[i]));	// set it still sorted
					}
					
					if (++check >= 100)
					{
						checkBreakpoint(breakpoint, ctxt);
						check = 0;
					}
					
					sets.add(ns);
				}
			}

			sets.add(new ValueSet());	// Add {}
		}

		return sets;
	}

	/**
	 * This is the (old) recursive power set algorithm.
	 */
//	private void powerGenerate(List<ValueSet> result, boolean[] flags, int n)
//	{
//		for (int i=0; i <= 1; ++i)
//		{
//			flags[n] = (i == 1);
//
//			if (n < flags.length - 1)
//			{
//				powerGenerate(result, flags, n+1);
//			}
//			else
//			{
//				ValueSet newset = new ValueSet(flags.length);
//
//				for (int f=0; f<flags.length; f++)
//				{
//					if (flags[f])
//					{
//						newset.addNoCheck(get(f));
//					}
//				}
//
//				result.add(newset);
//
//				checkBreakpoint(breakpoint, ctxt);
//			}
//		}
//	}

	/**
	 * Check whether we should drop into the debugger for long expansions.
	 */
	private void checkBreakpoint(Breakpoint breakpoint, Context ctxt)
	{
		// We check the interrupt level here, rather than letting the check
		// method do it, to avoid incrementing the hit count for the breakpoint
		// too many times.

		switch (Breakpoint.execInterruptLevel())
		{
			case Breakpoint.TERMINATE:
				throw new InternalException(4176, "Interrupted power set size " + size());
		
			case Breakpoint.PAUSE:
				if (breakpoint != null)
				{
					breakpoint.enterDebugger(ctxt);
				}
				break;
			
			case Breakpoint.NONE:
			default:
				break;	// carry on
		}
	}

	@Override
	public Object clone()
	{
		ValueSet copy = new ValueSet(size());

		for (Value v: this)
		{
			Value vcopy = (Value)v.clone();
			copy.add(vcopy);
		}

		copy.isSorted = isSorted;
		return copy;
	}
}
