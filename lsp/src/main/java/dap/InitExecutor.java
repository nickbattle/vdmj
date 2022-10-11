/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package dap;

import java.io.IOException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;

import vdmj.commands.Command;
import vdmj.commands.InitRunnable;
import workspace.Diag;
import workspace.EventHub;
import workspace.events.DAPEvaluateEvent;

public class InitExecutor extends AsyncExecutor
{
	private final String launchCommand;
	private final String defaultName;

	public InitExecutor(String id, DAPRequest request, String launchCommand, String defaultName)
	{
		super(id, request);
		this.launchCommand = launchCommand;
		this.defaultName = defaultName;
	}

	@Override
	protected void head() throws Exception
	{
		running = "initialization";
		
		if (defaultName != null)
		{
			manager.getInterpreter().setDefaultName(defaultName);
		}
		
		server.stdout(
				"*\n" +
				"* VDMJ " + Settings.dialect + " Interpreter\n" +
				(manager.getNoDebug() ? "" : "* DEBUG enabled\n") +
				"*\n\nDefault " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class") +
				" is " + manager.getInterpreter().getDefaultName() + "\n");
		
		server.stdout("Initialized in ... ");
	}

	@Override
	protected void exec() throws Exception
	{
		LexLocation.clearLocations();
		manager.getInterpreter().init();
	}

	@Override
	protected void tail(double time) throws Exception
	{
		server.stdout(time + " secs.\n\n");	// Init time

		if (launchCommand != null)
		{
			Command command = Command.parse(launchCommand);
			
			if (command instanceof InitRunnable)
			{
				EventHub.getInstance().publish(new DAPEvaluateEvent(launchCommand));
				InitRunnable initcmd = (InitRunnable)command;
				running = initcmd.getExpression();
				String launchResult = initcmd.initRun(request);
				server.stdout(initcmd.format(launchResult) + "\n");
			}
			else
			{
				server.stderr("Unsupported init command: " + launchCommand + "\n");
			}
		}
	}

	@Override
	protected void error(Throwable e) throws IOException
	{
		try
		{
			server.stderr(e.getMessage());
			server.stdout("Init terminated.\n");
			server.writeMessage(new DAPResponse("terminated", null));
		}
		catch (Throwable e1)
		{
			Diag.error("Error during error handler: " + e1);
		}
	}

	@Override
	protected void clean() throws IOException
	{
		running = null;

		if (launchCommand != null)
		{
			manager.clearInterpreter();
			server.writeMessage(new DAPResponse("terminated", null));
		}
	}
}
