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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import json.JSONObject;
import vdmj.commands.Command;
import vdmj.commands.ErrorCommand;
import vdmj.commands.ScriptRunnable;
import workspace.Diag;

public class ScriptExecutor extends AsyncExecutor
{
	private final String filename;
	private BufferedReader script;

	public ScriptExecutor(String id, DAPRequest request, String filename)
	{
		super(id, request);
		this.filename = filename;
	}

	@Override
	protected void head()
	{
		running = "script " + filename;
	}

	@Override
	protected void exec() throws Exception
	{
		this.script = new BufferedReader(new FileReader(filename));

		try
		{

			while (true)
			{
				String line = readLine();
				
				if (line == null)
				{
					return;
				}
				
				Command cmd = Command.parse(line);
				Diag.info("Script running %s", line);
				server.stdout(line + "\n");
				
				DAPMessageList response = null;
				
				if (cmd instanceof ScriptRunnable)
				{
					ScriptRunnable scmd = (ScriptRunnable)cmd;
					String result = scmd.scriptRun(request);
					Diag.info("Result = %s", result);
					server.stdout("= " + result + "\n");
				}
				else
				{
					response = cmd.run(request);
					
					if (cmd instanceof ErrorCommand)
					{
						Diag.info("Script aborted");
						server.stderr("ABORTED " + filename + "\n");
						server.writeMessage(new DAPResponse(request, false, null, null));
						return;
					}
					
					if (response.size() == 1 && response.get(0) instanceof DAPResponse)
					{
						DAPResponse dr = (DAPResponse) response.get(0);
						String message = dr.get("message");
						JSONObject body = dr.get("body");
						
						if (message != null)
						{
							server.stdout(message + "\n");
						}
						else if (body != null && body.containsKey("result"))
						{
							server.stdout(body.get("result"));
						}
					}
				}
			}
		}
		finally
		{
			if (script != null)
			{
				try
				{
					script.close();
				}
				catch (IOException e)
				{
					// ignore
				}
			}
		}
	}

	@Override
	protected void tail(double time) throws IOException
	{
		Diag.info("Completed script");
		String answer = "END " + filename + "\nExecuted in " + time + " secs.\n";
		server.writeMessage(new DAPResponse(request, true, null,
				new JSONObject("result", answer, "variablesReference", 0)));

	}

	@Override
	protected void error(Throwable e) throws IOException
	{
		Diag.info("Aborted script");
		server.writeMessage(new DAPResponse(request, false, "ABORTED " + filename + "\n" + e.getMessage(), null));
	}

	@Override
	protected void clean()
	{
		running = null;
	}

	private String readLine() throws IOException
	{
		StringBuilder line = new StringBuilder();
		line.append("\\");
		
		do
		{
			line.deleteCharAt(line.length() - 1);	// Remove trailing backslash
			String part = script.readLine();
			
			if (part != null)
			{
				line.append(part);
			}
			else
			{
				return null;
			}
		}
		while (line.length() > 0 && line.charAt(line.length() - 1) == '\\');

		return line.toString();
	}
}
