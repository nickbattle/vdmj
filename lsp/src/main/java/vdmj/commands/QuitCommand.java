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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package vdmj.commands;

import dap.AsyncExecutor;
import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPServer;
import lsp.CancellableThread;
import workspace.DAPWorkspaceManager;

public class QuitCommand extends AnalysisCommand
{
	public static final String HELP = "quit - end the debugging session";
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
			manager.stopDebugReader();
		}
		
		DAPServer.getInstance().setRunning(false);
		return manager.dapTerminate(request, false);
	}
	
	@Override
	public boolean notWhenRunning()
	{
		return false;
	}
}
