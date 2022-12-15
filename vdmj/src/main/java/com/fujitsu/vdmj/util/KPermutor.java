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

import com.fujitsu.vdmj.traces.PermuteArray;

/**
 * Iteratively produce all k-permutations from n items. Note that a k-permutation is
 * the collection of permutations of the k-combinations of the set.
 * 
 * Can be used as "for (int[] row: kpermutor)".
 */
public class KPermutor implements Iterator<int[]>, Iterable<int[]>
{
	private KCombinator combinator;
	private PermuteArray permutor;
	private int[] current;

    public KPermutor(int from, int choose)
    {
        if (from == 0)
        {
            throw new IllegalArgumentException("from");
        }
        
        if (choose <= 0 || choose > from)
        {
            throw new IllegalArgumentException("choose");
        }
        
        this.combinator = new KCombinator(from, choose);
        this.permutor = new PermuteArray(choose);
        this.current = null;
    }

    @Override
	public Iterator<int[]> iterator()
	{
		return this;
	}

	@Override
	public boolean hasNext()
    {
        return permutor.hasNext() || combinator.hasNext();
    }

    @Override
	public int[] next()
    {
    	if (current != null && permutor.hasNext())
    	{
    		return permuteCurrent();
    	}
    	else if (combinator.hasNext())
    	{
    		current = combinator.next();
    		permutor = new PermuteArray(current.length);
    		return permuteCurrent();
    	}
    	else
    	{
            throw new NoSuchElementException();
    	}
    }
    
    private int[] permuteCurrent()
    {
   		int[] index = permutor.next();
		int[] result = new int[index.length];
		
		for (int i=0; i<index.length; i++)
		{
			result[i] = current[index[i]];
		}
		
		return result;
    }
    
    public static void main(String[] args)
    {
    	KPermutor kpermutor = new KPermutor(4, 1);
    	
    	for (int[] row: kpermutor)
    	{
    		System.out.println(Arrays.toString(row));
    	}
    }
}