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

package vdmj.commands;

import dap.AsyncExecutor;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPServer;
import lsp.CancellableThread;
import vdmj.DAPDebugReader;
import workspace.DAPWorkspaceManager;
import workspace.Log;

public class QuitCommand extends Command
{
	public static final String[] HELP = { "quit", "quit - end the debugging session" };
	public static final String USAGE = "Usage: quit";
	
	public QuitCommand(String line)
	{
		if (!line.equals("quit") && !line.equals("q"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		DAPWorkspaceManager manager = DAPWorkspaceManager.getInstance();

		if (AsyncExecutor.currentlyRunning() != null)
		{
			CancellableThread.cancelAll();
			DAPDebugReader debugReader = manager.getDebugReader();
			
			if (debugReader != null)
			{
				int retries = 5;
				
				while (retries-- > 0 && !debugReader.isListening())
				{
					pause(200);
				}
				
				if (retries > 0)
				{
					debugReader.interrupt();	// Cause exchange to trip
					retries = 5;
					
					while (retries-- > 0 && manager.getDebugReader() != null)
					{
						pause(200);
					}
					
					if (retries == 0)
					{
						Log.error("DAPDebugReader interrupt did not work?");
					}
				}
				else
				{
					Log.error("DAPDebugReader did not catch interruption?");
				}
			}
		}
		
		DAPServer.getInstance().setRunning(false);
		return manager.terminate(request, false);
	}
	
	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
