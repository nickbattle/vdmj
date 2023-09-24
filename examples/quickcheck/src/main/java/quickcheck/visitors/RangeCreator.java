/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.visitors.TCTypeVisitor;
import com.fujitsu.vdmj.util.KCombinator;
import com.fujitsu.vdmj.values.ValueSet;

public abstract class RangeCreator extends TCTypeVisitor<ValueSet, Integer>
{
	protected final Context ctxt;
	protected final TCTypeSet done;

	protected RangeCreator(Context ctxt)
	{
		this.ctxt = ctxt;
		this.done = new TCTypeSet();
	}

	protected List<ValueSet> powerLimit(ValueSet source, int limit, boolean incEmpty)
	{
		// Generate a power set, up to limit values from the full power set.
		List<ValueSet> results = new Vector<ValueSet>();
		
		if (source.isEmpty())
		{
			if (incEmpty)
			{
				results.add(new ValueSet());	// Just {}
			}
		}
		else
		{
			int size = source.size();
			long count = 0;
			
			if (incEmpty)
			{
				results.add(new ValueSet());	// Add {}
				count++;
			}
			
			int from = (size > 3) ? 3 : size;	// Avoid very small sets?
			
			for (int ss = from; ss <= size; ss++)
			{
				for (int[] kc: new KCombinator(size, ss))
				{
					ValueSet ns = new ValueSet(ss);
	
					for (int i=0; i<ss; i++)
					{
						ns.add(source.get(kc[i]));
					}
					
					results.add(ns);
					
					if (++count >= limit)
					{
						return results;
					}
				}
			}
		}
	
		return results;
	}
	
	/**
	 * Return the lowest integer power of n that is <= limit. If n < 2,
	 * just return 1.
	 */
	protected int leastPower(int n, int limit)
	{
		int power = 1;

		if (n > 1)
		{
			int value = n;
			
			while (value < limit)
			{
				value = value * n;
				power++;
			}
		}
		
		return power;
	}
}
