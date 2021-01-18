/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package lsp;

import java.util.HashMap;
import java.util.Map;

import workspace.Log;

abstract public class CancellableThread extends Thread
{
	private static final Map<Object, CancellableThread> active = new HashMap<Object, CancellableThread>();
	protected final Object myId;
	protected boolean cancelled = false;
	
	public CancellableThread(Object myId)
	{
		this.myId = myId;
		active.put(myId, this);
	}
	
	@Override
	public void run()
	{
		try
		{
			Log.printf("Starting CancellableThread %s", getName());
			body();
		}
		finally
		{
			active.remove(myId);
			Log.printf("Completed CancellableThread %s", getName());
		}
	}
	
	public static void cancel(Object id)
	{
		CancellableThread thread = active.get(id);
		
		if (thread == null)
		{
			Log.error("Cannot cancel thread %s", id.toString());
		}
		else
		{
			thread.setCancelled();
		}
	}
	
	public void setCancelled()
	{
		cancelled = true;
		Log.printf("Thread %s cancel sent", myId.toString());
	}

	public static CancellableThread find(Object id)
	{
		return active.get(id);
	}

	abstract protected void body();
}
