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

import java.io.IOException;
import java.util.List;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.scheduler.SchedulableThread;

/**
 * A class to listen for and interact with multiple threads that are being debugged.
 */
public class DebugReader extends Thread
{
	private DebugLink link = DebugLink.getInstance();;
	private SchedulableThread debuggedThread = null;
	
	@Override
	public void run()
	{
		setName("DebugReader");
		
		while (link.waitForStop())
		{
			debuggedThread = link.getThreads().get(0);
			while (doCommand());
		}
	}

	private boolean doCommand()
	{
		try
		{
			Breakpoint bp = link.getBreakpoint(debuggedThread);
			
			if (bp != null)
			{
	    		Console.out.println("Stopped " + bp);
	       		Console.out.println(Interpreter.getInstance().getSourceLine(bp.location));
			}
			
			Console.out.printf("%s> ", debuggedThread.getName());
			Console.out.flush();
			String command = Console.in.readLine();
			
			if (command.equals("threads"))
			{
				doThreads();
				return true;
			}
			
			String response = link.command(debuggedThread, command);
			
			if (response.equals("resume"))
			{
				link.resume();
				return false;	// Call waitForStop
			}
			
			Console.out.println(response);
			return true;
		}
		catch (IOException e)
		{
			return false;
		}
	}

	private void doThreads()
	{
		List<SchedulableThread> threads = link.getThreads();
		
		if (threads.isEmpty())
		{
			Console.out.println("No threads?");
		}
		else if (threads.size() == 1)
		{
			debuggedThread = threads.get(0);
			Breakpoint bp = link.getBreakpoint(debuggedThread);

			if (bp != null)
			{
				Console.out.println(bp);
			}
		}
		else
		{
    		int i = 1;
    		
    		for (SchedulableThread th: threads)
    		{
    			Breakpoint bp = link.getBreakpoint(th);
    			Console.out.printf("%d: %s %s\n", i++, th.getName(), bp != null ? bp.toString() : "");
    		}
    		
    		int choice = 0;
    		
    		while (choice < 1 || choice > threads.size())
    		{
    			try
    			{
    				Console.out.printf("Select 1 to %d: ", threads.size());
    				Console.out.flush();
    				choice = Integer.parseInt(Console.in.readLine());
    			}
    			catch (Exception e)
    			{
    				Console.out.printf("Must choose 1 to %d\n", threads.size());
    			}
    		}
    		
    		debuggedThread = threads.get(choice-1);
		}
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
}
