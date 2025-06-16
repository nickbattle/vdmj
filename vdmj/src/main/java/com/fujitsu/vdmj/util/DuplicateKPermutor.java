/*******************************************************************************
 *
 *	Copyright (c) 2022, Nick Battle
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

package com.fujitsu.vdmj.util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Iteratively produce all k-permutations from n items, with duplicates.
 * 
 * Can be used as "for (int[] row: cpermutor)".
 */
public class DuplicateKPermutor implements Iterator<int[]>, Iterable<int[]>
{
	private int[] current;
	private int size;
	private boolean done;

    public DuplicateKPermutor(int from, int choose)
    {
        if (choose == 0)
        {
            throw new IllegalArgumentException("choose");
        }
        
        this.current = new int[choose];
        this.size = from;
        this.done = false;
    }

    @Override
	public Iterator<int[]> iterator()
	{
		return this;
	}

	@Override
	public boolean hasNext()
    {
        return !done;
    }

    @Override
	public int[] next()
    {
    	int[] result = copy();
    	
		for (int i=0; i<current.length; i++)
		{
			if (++current[i] < size)
			{
				break;
			}

			current[i] = 0;

			if (i == current.length-1)
			{
				done = true;
			}
		}
    	
    	return result;
    }
    
    private int[] copy()
    {
		int[] result = new int[current.length];
		System.arraycopy(current, 0, result, 0, current.length);
		return result;
    }
    
    public static void main(String[] args)
    {
    	DuplicateKPermutor cpermutor = new DuplicateKPermutor(4, 3);
    	
    	for (int[] row: cpermutor)
    	{
    		System.out.println(Arrays.toString(row));
    	}
    }
}