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

package vdmj;

import java.io.IOException;

import com.fujitsu.vdmj.debug.ConsoleDebugLink;
import com.fujitsu.vdmj.debug.DebugLink;
import com.fujitsu.vdmj.debug.DebugReason;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.values.CPUValue;

import dap.DAPResponse;
import dap.DAPServer;
import json.JSONObject;
import workspace.Log;

public class DAPDebugLink extends ConsoleDebugLink
{
	/** Singleton instance */
	private static DebugLink instance;

	private final DAPServer server;

	/**
	 * Get the singleton. 
	 */
	public static DebugLink getInstance()
	{
		if (instance == null)
		{
			instance = new DAPDebugLink();
			instance.setExecutor(new DAPDebugExecutor());
		}
		
		return instance;
	}
	
	private DAPDebugLink()
	{
		server = DAPServer.getInstance();
	}
	
	@Override
	public void newThread(CPUValue cpu)
	{
		try
		{
			server.writeMessage(new DAPResponse("thread",
				new JSONObject("reason", "started", "threadId", Thread.currentThread().getId())));
		}
		catch (IOException e)
		{
			Log.error(e);
		}
	}
	
	@Override
	public void complete(DebugReason reason, ContextException exception)
	{
		try
		{
			server.writeMessage(new DAPResponse("thread",
				new JSONObject("reason", "exited", "threadId", Thread.currentThread().getId())));
		}
		catch (IOException e)
		{
			Log.error(e);
		}
	}
}
