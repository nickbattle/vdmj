/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package simulation;

import com.fujitsu.vdmj.RemoteSimulation;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.runtime.ValueException;

/**
 * This simple example is designed to run with the sporadic.vdmrt test spec in
 * src/test/resources. It demonstrates setting parameters and changing the value
 * of runtime system variables during a simulation.
 * 
 * Execute in VDM by adding the project to the classpath and passing:
 * 
 *     -simulation simulation.TestSimulation
 * 
 * Then execute new Test().test() as usual. The output should be like:
 * 
 * > p new Test().test()
 * Last = 0
 * Updated = 1
 * Last = 1
 * Updated = 2
 * Last = 2
 * Updated = 3
 * Last = 3
 * Updated = 4
 * Last = 4
 * Updated = 5
 * Last = 5
 * Updated = 6
 * Last = 6
 * Updated = 7
 * = ()
 * Executed in 0.057 secs.
 */
public class TestSimulation extends RemoteSimulation
{
	@Override
	public void setup(ASTClassList classes)
	{
		setParameter(classes, "A", "MIN", 50);
		System.out.println("Set MIN to 50");
		setParameter(classes, "A", "MAX", 100);
		System.out.println("Set MAX to 100");
	}

	@Override
	public long step(long time)
	{
		try
		{
			Long last = getSystemIntegerValue("obj1", "last");
			System.out.println("Last = " + last);
			
			setSystemValue("obj1", "last", last + 1);
			last = getSystemIntegerValue("obj1", "last");
			System.out.println("Updated = " + last);
		}
		catch (ValueException e)
		{
			System.err.println(e);
		}
		
		return time + 1000;
	}
}
