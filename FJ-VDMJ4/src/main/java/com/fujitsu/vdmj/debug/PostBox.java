/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.debug;

import com.fujitsu.vdmj.scheduler.SchedulableThread;

/**
 * A class to exchange one object between threads.
 */
public class PostBox<T>
{
	private final SchedulableThread lock;
	private T data;
	
	public PostBox(SchedulableThread lock)
	{
		this.lock = lock;
	}
	
	public void post(T data)
	{
		synchronized(lock)
		{
			while (this.data != null)
			{
				try
				{
					lock.wait();
				}
				catch (InterruptedException e)
				{
					// ignore
				}
			}
			
			this.data = data;
			lock.notifyAll();
		}
	}
	
	public T pickup()
	{
		synchronized(lock)
		{
			while (this.data == null)
			{
				try
				{
					lock.wait();
				}
				catch (InterruptedException e)
				{
					// ignore
				}
			}
			
			T result = data;
			this.data = null;
			lock.notifyAll();
			return result;
		}	
	}
}
