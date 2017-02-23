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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Exchanger;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.scheduler.SchedulableThread;

/**
 * Implementation of Debugger for command line control on a console.
 */
public class DebuggerLink
{
	/** The ACK string */
	private static final String ACK = "ACK";
	
	/** The singleton instance */
	private static DebuggerLink instance = null;
	
	/** The threads that are currently stopped */
	private Map<SchedulableThread, Exchanger<String>> stopped = new HashMap<SchedulableThread, Exchanger<String>>();
	
	/**
	 * Get the singleton.
	 */
	public DebuggerLink getInstance()
	{
		if (instance == null)
		{
			instance = new DebuggerLink();
		}
		
		return instance;
	}
	
	private DebuggerLink()
	{
		// Not used
	}
	
	public Set<SchedulableThread> getThreads()
	{
		return stopped.keySet();
	}
	
	public synchronized void stopped()
	{
		Exchanger<String> exchanger = new Exchanger<String>();
		SchedulableThread thread = (SchedulableThread)Thread.currentThread();
		stopped.put(thread, exchanger);
		
		while (true)
		{
    		try
    		{
    			String request = exchanger.exchange(ACK);
    			String response = process(request);
    			String ack = exchanger.exchange(response);
    			
    			if (!ack.equals(ACK))
    			{
    				Console.out.println("Unexpected ack: " + ack);
    			}
    			
    			if (response.equals("continue"))
    			{
    				return;
    			}
    		}
    		catch (InterruptedException e)
    		{
    			// Assume this is the resume signal? So just return
    		}
		}
	}
	
	public String request(SchedulableThread thread, String command)
	{
		try
		{
			Exchanger<String> exchanger = stopped.get(thread);
			String ack = exchanger.exchange(command);
			
			if (!ack.equals(ACK))
			{
				Console.out.println("Unexpected ack: " + ack);
			}

			return exchanger.exchange(ACK);
		}
		catch (InterruptedException e)
		{
			// Client should never be interrupted
			return null;
		}
	}

	private String process(String request)
	{
		// TODO !
		return null;
	}
}
