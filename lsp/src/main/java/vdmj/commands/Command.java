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

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPServer;
import workspace.Diag;
import workspace.PluginRegistry;

abstract public class Command
{
	protected DAPServer server = DAPServer.getInstance();
	
	public static Command parse(String line)
	{
		if (line == null || line.isEmpty())
		{
			return new NullCommand();
		}

		String[] parts = line.split("\\s+");
		String name = parts[0];
		
		try
		{
			Diag.info("Trying to load command %s from plugins", name);
			Command cmd = PluginRegistry.getInstance().getCommand(line);
			
			if (cmd != null)
			{
				return cmd;
			}
			else
			{
				return new ErrorCommand("Unknown command '" + name.toLowerCase() + "'. Try help");
			}
		}
		catch (Exception e)
		{
			Diag.error(e);
			return new ErrorCommand("Error: " + e.getMessage());
		}
	}

	public abstract DAPMessageList run(DAPRequest request);

	public abstract boolean notWhenRunning();
	
	protected void pause(long ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch (InterruptedException e)
		{
			// ignore
		}
	}
}
