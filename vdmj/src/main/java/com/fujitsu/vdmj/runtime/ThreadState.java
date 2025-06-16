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

package com.fujitsu.vdmj.runtime;

import java.io.Serializable;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.CPUValue;

/**
 * A class to hold some runtime information for each thread.
 */

public class ThreadState implements Serializable
{
    private static final long serialVersionUID = 1L;
	public final long threadId;
	public final CPUValue CPU;

	private int atomic = 0;			// Don't reschedule if >0
	private int pure = 0;			// In a pure operation if >0

	public LexLocation stepline;	// Breakpoint stepping values
	public RootContext nextctxt;
	public Context outctxt;


	public ThreadState(CPUValue cpu)
	{
		this.threadId = Thread.currentThread().getId();
		this.CPU = cpu;
		init();
	}

	public void init()
	{
		setBreaks(null, null, null);
	}

	public synchronized void setBreaks(
		LexLocation stepline, RootContext nextctxt, Context outctxt)
	{
		this.stepline = stepline;
		this.nextctxt = nextctxt;
		this.outctxt = outctxt;
	}

	public synchronized boolean isStepping()
	{
		return stepline != null;
	}

	public void reschedule(Context ctxt, LexLocation location)
	{
		if (atomic == 0)
		{
			// Initialization doesn't occur from SchedulableThreads

			Thread current = Thread.currentThread();

			if (current instanceof SchedulableThread)
			{
				SchedulableThread s = (SchedulableThread)current;
				s.step(ctxt, location);
			}
		}
	}

	public synchronized void setAtomic(boolean atomic)
	{
		if (atomic)
		{
			this.atomic++;
		}
		else
		{
			this.atomic--;
		}
	}

	/**
	 * We set the pure mode when calling a function. The thread stays in pure mode until
	 * the outermost function call returns. Note that operations can only be called in
	 * this mode if they are also pure.
	 */
	public synchronized void setPure(boolean pure)
	{
		if (pure)
		{
			this.pure++;
		}
		else
		{
			this.pure--;
		}
	}
	
	public synchronized boolean isPure()
	{
		return pure > 0;
	}
}
