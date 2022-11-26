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

import com.fujitsu.vdmj.runtime.Breakpoint;

/**
 * Class to watch for keyboard interrupts.
 */
public class ConsoleKeyWatcher extends Thread
{
	private static final long PAUSE = 200;

	public ConsoleKeyWatcher()
	{
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
				
				if (link.isDebugging())		// ie. we're not stopped
				{
					int bytes = System.in.available();
					
					while (bytes-- > 0)
					{
						int key = System.in.read();
						
						switch (key)
						{
							case 'p':
								System.out.println("Pausing...");
								Breakpoint.setExecInterrupt(Breakpoint.PAUSE);
								break;
								
							case 'q':
								System.out.println("Terminating...");
								Breakpoint.setExecInterrupt(Breakpoint.TERMINATE);
								break;
								
							case '\n':
								break;
								
							default:
								System.out.println("p <enter> to pause");
								System.out.println("q <enter> to interrupt");
								break;
						}
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
