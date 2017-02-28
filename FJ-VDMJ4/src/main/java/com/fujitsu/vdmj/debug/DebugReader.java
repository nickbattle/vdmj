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

import com.fujitsu.vdmj.lex.LexLocation;
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
			debuggedThread = link.getDebugThread();		// Initially bp thread
			while (doCommand());
		}
	}

	private boolean doCommand()
	{
		try
		{
			Breakpoint bp = link.getBreakpoint(debuggedThread);
			LexLocation loc = link.getLocation(debuggedThread);
			
			if (bp != null)
			{
	    		Console.out.println("Stopped " + bp);
	       		Console.out.println(Interpreter.getInstance().getSourceLine(bp.location));
			}
			else if (loc == null)
			{
				Console.out.println("Thread has not yet started");
			}
			else
			{
				Console.out.println(Interpreter.getInstance().getSourceLine(loc));
			}
			
			String command = null;
			
			while (command == null || command.length() == 0)
			{
    			Console.out.printf("%s> ", debuggedThread.getName());
    			Console.out.flush();
    			command = Console.in.readLine().trim();
			}
			
			if (command.equals("threads"))
			{
				doThreads();
				return true;
			}
			else if (command.startsWith("thread "))
			{
				doThread(command);
				return true;
			}
			else
			{
    			String response = link.command(debuggedThread, command);
    			
    			if (response.equals("resume"))
    			{
    				link.resume();
    				return false;	// Call waitForStop again
    			}
    			else if (response.equals("quit"))
    			{
    				link.kill();
    				return false;
    			}
    			
    			Console.out.println(response);
    			return true;
			}
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
		else
		{
    		int i = 1;
    		Console.out.println("----");
    		
    		for (SchedulableThread th: threads)
    		{
    			Breakpoint bp = link.getBreakpoint(th);
    			Console.out.printf("%d: %s %s\n", i++, th.getName(), bp != null ? bp.toString() : "");
    		}
    		
    		Console.out.println("----");
		}
	}
	
	private void doThread(String line)
	{
		String[] parts = line.split("\\s+");

		if (parts.length != 2 || !parts[0].equals("thread"))
		{
			Console.out.println("Usage: thread <n>");
			return;
		}

		try
		{
			int n = Integer.parseInt(parts[1]);
			List<SchedulableThread> threads = link.getThreads();

			if (n < 1 || n > threads.size())
			{
				Console.out.printf("Thread number 1 to %d\n", threads.size());
			}
			else
			{
				debuggedThread = threads.get(n - 1);
			}
		}
		catch (NumberFormatException e)
		{
			Console.out.println("Usage: thread <n>");
		}
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
}
