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

import java.util.List;
import java.util.Vector;

/**
 * A class to collect test sequences that failed, in order to filter subsequent tests.
 */
public class TraceFilter
{
	private List<CallSequence> failedTests = new Vector<CallSequence>();
	private List<Integer> failedStems = new Vector<Integer>();
	private List<Integer> failedNumbers = new Vector<Integer>();
	
	public int getFilter(CallSequence test)
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

	public void check(List<Object> result, CallSequence test, int n)
	{
		if (result.get(result.size()-1) != Verdict.PASSED)
		{
			failedTests.add(test);
			failedStems.add(result.size() - 1);
			failedNumbers.add(n);
		}
	}
}
