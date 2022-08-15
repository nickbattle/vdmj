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

package com.fujitsu.vdmj.scheduler;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.fujitsu.vdmj.RemoteSimulation;

public class ResourceScheduler implements Serializable
{
    private static final long serialVersionUID = 1L;

	public String name = "scheduler";
	private List<Resource> resources = new LinkedList<Resource>();
	private static boolean stopping = false;
	private static MainThread mainThread = null;

	public void init()
	{
		resources.clear();
		stopping = false;
	}

	public void reset()
	{
		for (Resource r: resources)
		{
			r.reset();
		}
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void register(Resource resource)
	{
		resources.add(resource);
		resource.setScheduler(this);
	}

	public void unregister(Resource resource)
	{
		resources.remove(resource);
		resource.setScheduler(null);
	}

	public void start(MainThread main)
	{
		mainThread = main;
		stopping = false;

		boolean idle = true;
		long nextSimulationStop = Long.MAX_VALUE;
		RemoteSimulation simulation = RemoteSimulation.getInstance();		

		if (simulation != null)
		{
			nextSimulationStop = simulation.step(SystemClock.getWallTime());
		}

		do
		{
			long minstep = Long.MAX_VALUE;
			idle = true;

			for (Resource resource: resources)
			{
				if (resource.reschedule())
				{
					idle = false;
				}
				else
				{
					long d = resource.getMinimumTimestep();

					if (d < minstep)
					{
						minstep = d;
					}
				}
			}

			if (idle && minstep >= 0 && minstep < Long.MAX_VALUE)
			{
				SystemClock.advance(minstep);

				if (simulation != null && SystemClock.getWallTime() >= nextSimulationStop)
				{
					nextSimulationStop = simulation.step(SystemClock.getWallTime());
				}

				for (Resource resource: resources)
				{
					resource.advance();
				}

				idle = false;
			}
		}
		while (!idle && main.getRunState() != RunState.COMPLETE && main.getException() == null);

		stopping = true;

		if (main.getRunState() != RunState.COMPLETE && main.getException() == null)
		{
    		for (Resource resource: resources)
    		{
    			if (resource.hasActive())
    			{
					SchedulableThread.signalAll(Signal.DEADLOCKED);
					main.setException(new Exception("DEADLOCK detected"));

					while (main.isAlive())
					{
						try
                        {
	                        Thread.sleep(100);
                        }
                        catch (InterruptedException e)
                        {
	                        // ?
                        }
					}

    				break;
    			}
    		}
		}
	}

	public String getStatus()
	{
		StringBuilder sb = new StringBuilder();
		String sep = "";

		for (Resource r: resources)
		{
			sb.append(sep);
			String s = r.getStatus();

			if (!s.isEmpty())
			{
				sb.append(s);
				sep = "\n";
			}
			else
			{
				sep = "";
			}
		}

		return sb.toString();
	}

	public static boolean isStopping()
	{
		return stopping;
	}

	public static void setException(Exception e)
	{
		mainThread.setException(e);
	}
}
