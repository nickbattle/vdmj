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

abstract public class AnalysisCommand
{
	protected DAPServer server = DAPServer.getInstance();
	protected final String[] argv;
	
	protected AnalysisCommand(String line)
	{
		argv = line.split("\\s+");
	}

	/**
	 * Run the command. The JSON returned will be sent back to the Client, if not null.
	 */
	public abstract DAPMessageList run(DAPRequest request);

	/**
	 * Returns true if this command should not be executed while another is running.
	 */
	public abstract boolean notWhenRunning();

	/**
	 * Returns true if this command should not be executed when spec has errors or unsaved changes.
	 */
	public boolean notWhenDirty()
	{
		return false;
	}

	/**
	 * Create an AnalysisCommand instance from the line passed, using the PluginRegistry.
	 * Errors should be caught and turned into ErrorCommands, which print a message when
	 * executed.
	 */
	public static AnalysisCommand parse(String line)
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
			AnalysisCommand cmd = PluginRegistry.getInstance().getCommand(line);
			
			if (cmd != null)
			{
				return cmd;		// Usage is ok
			}
			else
			{
				return new ErrorCommand("Unknown command '" + name.toLowerCase() + "'. Try help");
			}
		}
		catch (Throwable e)
		{
			Diag.error(e);
			return new ErrorCommand("Error: " + e.getMessage());
		}
	}
}
