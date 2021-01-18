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
import com.fujitsu.vdmj.runtime.Interpreter;

import json.JSONObject;

public class InitExecutor extends AsyncExecutor
{
	public InitExecutor(String id, DAPRequest request)
	{
		super(id, request);
	}

	@Override
	protected void head() throws IOException
	{
		server.writeMessage(new DAPResponse("output", new JSONObject("output",
				"*\n" +
				"* VDMJ " + Settings.dialect + " Interpreter\n" +
				(manager.getNoDebug() ? "" : "* DEBUG enabled\n") +
				"*\n\nDefault " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class") +
				" is " + Interpreter.getInstance().getDefaultName() + "\n")));
		
		server.writeMessage(new DAPResponse("output", new JSONObject("output", "Initialized in ... ")));
	}

	@Override
	protected void exec() throws Exception
	{
		Interpreter.getInstance().init();
	}

	@Override
	protected void tail(double time) throws IOException
	{
		server.writeMessage(new DAPResponse("output", new JSONObject("output", time + " secs.\n")));
	}

	@Override
	protected void error(Exception e) throws IOException
	{
		server.writeMessage(new DAPResponse(request, false, e.getMessage(), null));
		server.writeMessage(new DAPResponse("output", new JSONObject("output", "Init terminated.")));
	}
}
