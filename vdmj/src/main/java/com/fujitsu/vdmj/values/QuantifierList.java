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

package com.fujitsu.vdmj.values;

import java.util.Vector;

import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.PatternMatchException;


public class QuantifierList extends Vector<Quantifier>
{
	private int count;
	private int[] size;
	private int[] next;
	private NameValuePairList result = null;
	private boolean done = false;

	public void init(Context ctxt, boolean allPossibilities)
	{
		count = size();
		size = new int[count];
		next = new int[count];
		boolean someData = false;
		boolean oneHasNoData = false;

		for (int i=0; i<count; i++)
		{
			size[i] = get(i).size(ctxt, allPossibilities);
			someData = someData || size[i] > 0;
			oneHasNoData = oneHasNoData || size[i] == 0;
			next[i] = 0;
		}

		done = count == 0 || !someData || oneHasNoData;
	}

	private void permute()
	{
		for (int i=0; i<count; i++)
		{
			if (++next[i] < size[i])
			{
				break;
			}

			next[i] = 0;

			if (i == count-1)
			{
				done = true;
			}
		}
	}

	public NameValuePairList next()
	{
		return result;
	}

	public boolean hasNext()
	{
		while (!done)
		{
			try
			{
           		result = new NameValuePairList();

        		for (int i=0; i<count; i++)
        		{
        			Quantifier q = get(i);
        			result.addAll(q.get(next[i]));
        		}

        		permute();
        		return true;
			}
			catch (PatternMatchException e)
			{
				permute();		// Bad matches are ignored
			}
		}

		return false;
	}

	public boolean finished()
	{
		return done;
	}
}
