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
import java.util.Collections;
import java.util.List;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Breakpoint;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.Tracepoint;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.values.OperationValue;

/**
 * A class to listen for and interact with multiple threads that are being debugged.
 */
public class ConsoleDebugReader extends Thread implements TraceCallback
{
	private ConsoleDebugLink link = null;
	private SchedulableThread debuggedThread = null;
	private LexLocation lastLoc = null;
	private SchedulableThread lastThread = null;
	
	public ConsoleDebugReader() throws Exception
	{
		link = (ConsoleDebugLink)DebugLink.getInstance();
	}
	
	@Override
	public void run()
	{
		setName("DebugReader");
		link.setTraceCallback(this);
		
		while (link.waitForStop())
		{
			lastThread = debuggedThread;
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
			
			if (bp != null && bp.number != 0)	// Zero is used for next/step breakpoints.
			{
				Console.out.println("Stopped " + bp);
				Console.out.println(Interpreter.getInstance().getSourceLine(bp.location));
				lastLoc = bp.location;
			}
			else if (loc == null)
			{
				Console.out.printf("Thread %s has not yet started\n", debuggedThread.getName());
			}
			else	// Only print the source if we have moved
			{
				if (!debuggedThread.equals(lastThread) || !loc.equals(lastLoc))
				{
					Console.out.println(Interpreter.getInstance().getSourceLine(loc));
					lastLoc = loc;
				}
			}
			
			DebugCommand command = null;
			
			while (command == null)
			{
    			Console.out.printf("%s> ", debuggedThread.getName());
    			Console.out.flush();
    			command = DebugParser.parse(Console.in.readLine().trim());
			}
			
			switch (command.getType())
			{
				case THREADS:
					doThreads();
					return true;

				case THREAD:
					doThread(command);
					return true;

				default:
				{
					DebugCommand response = link.sendCommand(debuggedThread, command);

					switch (response.getType())
					{
						case RESUME:
							link.resumeThreads();
							return false;

						case STOP:
						case QUIT:
							link.killThreads();
							return false;

						default:
							Console.out.println(response); // toString of commands are sensible
							return true;
					}
				}
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
		Collections.sort(threads);
		
		if (threads.isEmpty())
		{
			Console.out.println("No threads?");
		}
		else
		{
    		int maxName = 0;
    		long maxNum = 0;
    		
    		for (SchedulableThread th: threads)
    		{
    			if (th.getName().length() > maxName)
    			{
    				maxName = th.getName().length();
    			}
    			
    			if (th.getId() > maxNum)
    			{
    				maxNum = th.getId();
    			}
    		}
    		
    		int width = (int)Math.floor(Math.log10(maxNum)) + 1;
    		
    		for (SchedulableThread th: threads)
    		{
    			Breakpoint bp = link.getBreakpoint(th);
    			OperationValue guard = link.getGuardOp(th);
    			LexLocation loc = link.getLocation(th);
    			String info = "(not started)";
    			
    			if (bp != null)
    			{
    				info = bp.toString();
    			}
    			else if (guard != null)
    			{
    				info = "sync: " + guard.name.getName() + " " + loc;
    			}
    			else if (loc != null)
    			{
    				info = loc.toString();
    			}
    			
    			String format = String.format("%%%dd: %%-%ds  %%s\n", width, maxName);
    			Console.out.printf(format, th.getId(), th.getName(), info);
    		}
    		
    		Console.out.println();
		}
	}
	
	private void doThread(DebugCommand cmd)
	{
		Integer n = (Integer)cmd.getPayload();

		for (SchedulableThread th: link.getThreads())
		{
			if (th.getId() == n)
			{
				debuggedThread = th;
				return;
			}
		}

		Console.out.println("No such thread Id - try 'threads'");
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
	
	@Override
	public void tracepoint(Context ctxt, Tracepoint tp)
	{
		if (tp.condition == null)
		{
			String s = "Reached trace point [" + tp.number + "]";
			Console.out.println(Thread.currentThread().getName() + ": " + s);
		}
		else
		{
			String result = null;
			
			try
			{
				result = tp.condition.eval(ctxt).toString();
			}
			catch (Exception e)
			{
				result = e.getMessage();
			}
			
			String s = tp.trace + " = " + result + " at trace point [" + tp.number + "]";
			Console.out.println(Thread.currentThread().getName() + ": " + s);
		}
	}
}
