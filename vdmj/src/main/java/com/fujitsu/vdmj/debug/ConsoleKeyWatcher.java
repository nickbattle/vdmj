/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.debug;

import java.io.IOException;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Breakpoint;

/**
 * Class to watch for keyboard interrupts.
 */
public class ConsoleKeyWatcher extends Thread
{
	private static final long PAUSE = 200;
	private final String expression;
	private final long startTime;

	public ConsoleKeyWatcher(String expression)
	{
		this.expression = expression;
		this.startTime = System.currentTimeMillis();
		setName("KeyWatcher");
	}
	
	@Override
	public void run()
	{
		ConsoleDebugLink link = (ConsoleDebugLink)DebugLink.getInstance();

		while (true)
		{
			try
			{
				Thread.sleep(PAUSE);
				
				if (link.isDebugging() && Console.in.ready())	// ie. we're not stopped, and there's key data
				{
					switch (Console.in.readLine())
					{
						case "p":
						case "pause":
							Console.out.println("Pausing...");
							Breakpoint.setExecInterrupt(Breakpoint.PAUSE);
							break;
							
						case "q":
						case "quit":
						case "k":
						case "kill":
							Console.out.println("Terminating...");
							Breakpoint.setExecInterrupt(Breakpoint.TERMINATE);
							break;
							
						default:
							long now = System.currentTimeMillis();
					  		double runtime = (double)(now - startTime)/1000;
					  	 
							Console.out.println("Running " + expression + " for " + runtime + "s");
							Console.out.println("[p]ause to pause execution");
							Console.out.println("[q]uit or [k]ill to cancel");
							break;
					}
				}
			}
			catch (InterruptedException e)
			{
				return;
			}
			catch (IOException e)
			{
				// ignore
			}
		}
	}
}
