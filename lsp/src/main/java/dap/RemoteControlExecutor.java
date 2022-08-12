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
import com.fujitsu.vdmj.RemoteControl;
import com.fujitsu.vdmj.RemoteInterpreter;
import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Interpreter;

public class RemoteControlExecutor extends AsyncExecutor
{
	private final String remoteControl;
	private final String defaultName;

	public RemoteControlExecutor(String id, DAPRequest request, String remoteControl, String defaultName)
	{
		super(id, request);
		this.remoteControl = remoteControl;
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
				" is " + manager.getInterpreter().getDefaultName() + "\n" +
				"Remote control class = " + remoteControl + "\n");
		
		server.stdout("Initialized in ... ");
	}

	@Override
	protected void exec() throws Exception
	{
		LexLocation.clearLocations();
		manager.getInterpreter().init();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void tail(double time) throws Exception
	{
		server.stdout(time + " secs.\n\n");	// Init time

		try
		{
			Class<RemoteControl> remoteClass = (Class<RemoteControl>) ClassLoader.getSystemClassLoader().loadClass(remoteControl);
			RemoteControl remote = remoteClass.getDeclaredConstructor().newInstance();
			Interpreter i = Interpreter.getInstance();

			running = remoteControl;
			server.stdout("Starting " + remoteControl + "\n");
			remote.run(new RemoteInterpreter(i));
		}
		catch (ClassNotFoundException e)
		{
			throw new Exception("Cannot locate class " + remoteControl + " on the CLASSPATH");
		}
		catch (ClassCastException e)
		{
			throw new Exception(remoteControl + " does not implement RemoteControl interface");
		}
		catch (InstantiationException e)
		{
			throw new Exception("Cannot instantiate " + remoteControl);
		}
	}

	@Override
	protected void error(Throwable e) throws IOException
	{
		server.stderr(e.getMessage());
		server.stdout("Remote control terminated.\n");
	}

	@Override
	protected void clean() throws IOException
	{
		running = null;

		manager.clearInterpreter();
		server.writeMessage(new DAPResponse("terminated", null));
	}
}
