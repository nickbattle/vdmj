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

package vdmj.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.DAPResponse;
import json.JSONObject;
import workspace.Diag;

public class ScriptCommand extends Command implements ScriptRunnable
{
	public static final String USAGE = "Usage: script <file>";
	public static final String[] HELP = { "script", "script <file> - run commands from file" };
	
	private final File scriptFile;
	private final BufferedReader script;

	public ScriptCommand(String line)
	{
		String[] parts = line.split("\\s+", 2);
		
		if (parts.length == 2)
		{
			try
			{
				scriptFile = new File(parts[1]);
				script = new BufferedReader(new FileReader(scriptFile));
				Diag.info("Opened script file %s", scriptFile);
			}
			catch (FileNotFoundException e)
			{
				throw new IllegalArgumentException(e.getMessage());
			}
		}
		else
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			while (true)
			{
				String line = readLine();
				
				if (line == null)
				{
					Diag.info("Completed script");
					server.stdout("END " + scriptFile.getName() + "\n");
					return new DAPMessageList(request);
				}
				
				Command cmd = Command.parse(line);
				Diag.info("Script running %s", line);
				server.stdout(line + "\n");
				
				DAPMessageList response = null;
				
				if (cmd instanceof ScriptRunnable)
				{
					ScriptRunnable scmd = (ScriptRunnable)cmd;
					String result = scmd.scriptRun(request);
					Diag.info("Result = ", result);
					server.stdout("= " + result + "\n");
				}
				else
				{
					response = cmd.run(request);
					
					if (cmd instanceof ErrorCommand)
					{
						Diag.info("Script aborted");
						server.stderr("ABORTED " + scriptFile.getName() + "\n");
						return response;
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
		catch (IOException e)
		{
			Diag.info("Script aborted");
			server.stderr("ABORTED " + scriptFile.getName() + "\n");
			return new DAPMessageList(request, e);
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
				script.close();
				return null;
			}
		}
		while (line.length() > 0 && line.charAt(line.length() - 1) == '\\');

		return line.toString();
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}

	@Override
	public String scriptRun(DAPRequest request) throws IOException
	{
		return "Cannot nest scripts";
	}
}
