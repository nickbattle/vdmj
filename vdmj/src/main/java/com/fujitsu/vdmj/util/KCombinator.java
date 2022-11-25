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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iteratively produce all k-combinations from n items.
 * Can be used as "for (int[] row: kcombinator)".
 */
public class KCombinator implements Iterator<int[]>, Iterable<int[]>
{
    private int[] items;
    private int choose;
    private boolean finished;
    private int[] current;

    public KCombinator(int from, int choose)
    {
        if (from == 0)
        {
            throw new IllegalArgumentException("from");
        }
        if (choose <= 0 || choose > from)
        {
            throw new IllegalArgumentException("choose");
        }
        
        this.items = new int[from];
        
        for (int i=0; i<from; i++)
        {
        	items[i] = i;
        }
        
        this.choose = choose;
        this.finished = false;
    }

    @Override
	public Iterator<int[]> iterator()
	{
		return this;
	}

	@Override
	public boolean hasNext()
    {
        return !finished;
    }

    @Override
	public int[] next()
    {
        if (!hasNext())
        {
            throw new NoSuchElementException();
        }

        if (current == null)
        {
            current = new int[choose];
            
            for (int i = 0; i < choose; i++)
            {
                current[i] = i;
            }
        }

        int[] result = new int[choose];
        
        for (int i = 0; i < choose; i++)
        {
            result[i] = items[current[i]];
        }

        int n = items.length;
        finished = true;

        for (int i = choose - 1; i >= 0; i--)
        {
            if (current[i] < n - choose + i)
            {
                current[i]++;
                
                for (int j = i + 1; j < choose; j++)
                {
                    current[j] = current[i] - i + j;
                }
                
                finished = false;
                break;
            }
        }

        return result;
    }
    
    public static void main(String[] args)
    {
    	KCombinator c = new KCombinator(4, 2);
    	
    	for (int[] comb: c)
    	{
    		System.out.println(Arrays.toString(comb));
    	}
    }
}