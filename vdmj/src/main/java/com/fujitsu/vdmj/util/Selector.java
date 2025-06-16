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

package com.fujitsu.vdmj.util;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Produce every selection of one value from an array of limits.
 * 
 * Callable as an iterator, "for (int[] row: selector)"
 */
public class Selector implements Iterator<int[]>, Iterable<int[]>
{
	private final int[] limit;
	private final int count;
	private final int[] current;

	private boolean done = false;

	public Selector(int[] limits)
	{
		this.limit = limits;
		this.count = limits.length;
		this.current = new int[count];
		this.done = true;
		
		for (int i=0; i<count; i++)
		{
			if (limits[i] > 0)
			{
				this.done = false;
				break;
			}
		}
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

		for (int i=0; i<count; i++)
		{
			if (++current[i] < limit[i])
			{
				done = false;
				break;
			}

			current[i] = 0;

			if (i == count-1)
			{
				done = true;
			}
		}

		return result;
	}

    private int[] copy()
    {
		int[] result = new int[count];
		System.arraycopy(current, 0, result, 0, count);
		return result;
    }
    
    public static void main(String[] args)
    {
    	int[] limits = { 3, 4 };
    	Selector selector = new Selector(limits);
    	
    	for (int[] row: selector)
    	{
    		System.out.println(Arrays.toString(row));
    	}
    }
}
