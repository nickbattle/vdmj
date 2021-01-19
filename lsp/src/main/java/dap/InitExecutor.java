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
 *
 ******************************************************************************/

package dap;

import java.io.IOException;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

public class InitExecutor extends AsyncExecutor
{
	private String launchCommand;

	public InitExecutor(String id, DAPRequest request, String launchCommand)
	{
		super(id, request);
		this.launchCommand = launchCommand;
	}

	@Override
	protected void head() throws IOException
	{
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
		manager.getInterpreter().init();
	}

	@Override
	protected void tail(double time) throws Exception
	{
		server.stdout(time + " secs.\n");

		if (launchCommand != null)
		{
			if (launchCommand.startsWith("p ") || launchCommand.startsWith("print "))
			{
				launchCommand = launchCommand.substring(launchCommand.indexOf(' ') + 1);
			}
			
			String launchResult = manager.getInterpreter().execute(launchCommand).toString();
			server.stdout(launchCommand + " = " + launchResult + "\n");
			server.stdout("Evaluation complete.\n");
		}
	}

	@Override
	protected void error(Exception e) throws IOException
	{
		server.stderr(e.getMessage());
		server.stdout("Init terminated.");
		manager.clearInterpreter();
		server.writeMessage(new DAPEvent("terminated", null));
	}

	@Override
	protected void clean() throws IOException
	{
		if (launchCommand != null)
		{
			manager.clearInterpreter();
			server.writeMessage(new DAPEvent("terminated", null));
		}
	}
}
