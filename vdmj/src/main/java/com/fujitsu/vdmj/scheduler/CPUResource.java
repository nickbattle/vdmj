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

package com.fujitsu.vdmj.scheduler;

import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.scheduler.SystemClock.TimeUnit;
import com.fujitsu.vdmj.values.ObjectValue;

public class CPUResource extends Resource
{
    private static final long serialVersionUID = 1L;
	private static int nextCPU = 0;
	public static CPUResource vCPU = null;

	private final int cpuNumber;
	private final double clock;

	private SchedulableThread swappedIn = null;

	public CPUResource(SchedulingPolicy policy, double clock)
	{
		super(policy);
		this.cpuNumber = nextCPU++;
		this.clock = clock;

		swappedIn = null;

		if (cpuNumber == 0)
		{
			vCPU = this;
		}
	}

	public static void init()
	{
		nextCPU = 0;
		vCPU = null;
	}

	@Override
	public void reset()
	{
		swappedIn = null;
		policy.reset();
		PeriodicThread.reset();
	}

	// Find the next thread to schedule and run one timeslice. The return
	// value indicates whether we ran something (true) or are idle (false,
	// may be due to a time step, including a swap delay).

	@Override
	public boolean reschedule()
	{
		if (swappedIn != null &&
			swappedIn.getRunState() == RunState.TIMESTEP)
		{
			return false;	// Can't schedule anything else and we're idle.
		}

		if (policy.reschedule())
		{
			SchedulableThread best = policy.getThread();

			if (swappedIn != best)
			{
				if (swappedIn != null)
				{
	    			RTLogger.log(
	    				"ThreadSwapOut -> id: " + swappedIn.getId() +
	    				objRefString(swappedIn.getObject()) +
	    				" cpunm: " + cpuNumber +
	    				" overhead: " + 0);
				}

				long delay = SystemClock.getWallTime() - best.getSwapInBy();

				if (best.getSwapInBy() > 0 && delay > 0)
				{
		        	RTLogger.log(
		        		"DelayedThreadSwapIn -> id: " + best.getId() +
		        		objRefString(best.getObject()) +
		        		" delay: " + delay +
		        		" cpunm: " + cpuNumber +
		        		" overhead: " + 0);
				}
				else
				{
    				RTLogger.log(
    					"ThreadSwapIn -> id: " + best.getId() +
    					objRefString(best.getObject()) +
    					" cpunm: " + cpuNumber +
    					" overhead: " + 0);
				}
			}

			swappedIn = best;
			swappedIn.runslice(policy.getTimeslice());

			switch (swappedIn.getRunState())
			{
				case COMPLETE:
        			RTLogger.log(
        				"ThreadSwapOut -> id: " + swappedIn.getId() +
        				objRefString(swappedIn.getObject()) +
        				" cpunm: " + cpuNumber +
        				" overhead: " + 0);

        			RTLogger.log(
    					"ThreadKill -> id: " + swappedIn.getId() +
    					" cpunm: " + cpuNumber);

        			swappedIn = null;
        			return true;	// We may be able to run other threads

				case TIMESTEP:
					return false;	// We're definitely idle

				default:
					return true;	// We may be able to run other threads
			}
		}
		else
		{
			return false;
		}
	}

	@Override
	public long getMinimumTimestep()
	{
		long minTime = policy.timeToNextAlarm();

		if (swappedIn != null)
		{
			switch (swappedIn.getRunState())
			{
				case TIMESTEP:
					long step = swappedIn.getTimestep();

					if (step < minTime)
					{
						minTime = step;
					}
					break;

				case RUNNING:
					return -1;		// Can't timestep

				default:
					break;
			}
		}

		return minTime;
	}

	public void createThread(SchedulableThread th)
	{
		RTLogger.log(
			"ThreadCreate -> id: " + th.getId() +
			" period: " + th.isPeriodic() +
			objRefString(th.getObject()) +
			" cpunm: " + cpuNumber);
	}

	public void deploy(ObjectValue object)
	{
		RTLogger.log(
			"DeployObj -> objref: " + object.objectReference +
			" clnm: \"" + object.type + "\"" +
			" cpunm: " + cpuNumber);
	}

	private String objRefString(ObjectValue obj)
	{
		return
			" objref: " + (obj == null ? "nil" : obj.objectReference) +
			" clnm: " + (obj == null ? "nil" : ("\"" + obj.type + "\""));
	}

	public long getCyclesDuration(long cycles)
	{
		return isVirtual() ? 0 : SystemClock.timeToInternal(TimeUnit.seconds, Double.valueOf(cycles) / clock);
	}

	public int getNumber()
	{
		return cpuNumber;
	}

	@Override
	public boolean isVirtual()
	{
		return this == vCPU;
	}

	@Override
	public String getStatus()
	{
		return policy.getStatus();
	}
}
