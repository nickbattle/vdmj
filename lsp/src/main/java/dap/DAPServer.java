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

package dap;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsoleWriter;

import dap.handlers.DebuggingHandler;
import dap.handlers.DisconnectHandler;
import dap.handlers.EvaluateHandler;
import dap.handlers.InitializeHandler;
import dap.handlers.LaunchHandler;
import dap.handlers.PauseHandler;
import dap.handlers.SetBreakpointsHandler;
import dap.handlers.SourceHandler;
import dap.handlers.StackTraceHandler;
import dap.handlers.TerminateHandler;
import dap.handlers.ThreadsHandler;
import json.JSONObject;
import json.JSONServer;
import workspace.DAPWorkspaceManager;
import workspace.Diag;

public class DAPServer extends JSONServer
{
	private static DAPServer INSTANCE = null;
	private final DAPDispatcher dispatcher;
	private boolean running = false;
	
	public DAPServer(Dialect dialect, Socket socket) throws IOException
	{
		super("DAP", socket.getInputStream(), socket.getOutputStream());
		
		INSTANCE = this;
		this.dispatcher = getDispatcher();
		
		DAPWorkspaceManager.getInstance();		// Just set up
	}
	
	public static DAPServer getInstance()
	{
		return INSTANCE;
	}
	
	private DAPDispatcher getDispatcher() throws IOException
	{
		DAPDispatcher dispatcher = DAPDispatcher.getInstance();
		
		dispatcher.register(new InitializeHandler(), "initialize");
		dispatcher.register(new LaunchHandler(), "launch");
		dispatcher.register(new InitializeHandler(), "configurationDone");
		dispatcher.register(new ThreadsHandler(), "threads");
		dispatcher.register(new SetBreakpointsHandler(), "setBreakpoints", "setExceptionBreakpoints", "setFunctionBreakpoints");
		dispatcher.register(new EvaluateHandler(), "evaluate");
		dispatcher.register(new StackTraceHandler(), "stackTrace");
		dispatcher.register(new DisconnectHandler(), "disconnect");
		dispatcher.register(new TerminateHandler(), "terminate");
		dispatcher.register(new PauseHandler(), "pause");
		dispatcher.register(new SourceHandler(), "source");
		
		dispatcher.register(new DebuggingHandler(),
			"continue", "stepIn", "stepOut", "next", "scopes", "variables");

		dispatcher.register(new UnknownHandler());
		
		return dispatcher;
	}

	public boolean isRunning()
	{
		return running;
	}

	public void setRunning(boolean arg)
	{
		running = arg;
	}

	public void run() throws IOException
	{
		running = true;
		Console.init(Charset.forName("UTF-8"), getOutConsoleWriter(), getErrConsoleWriter());

		while (running)
		{
			JSONObject message = readMessage();
			
			if (message == null)	// EOF
			{
				Diag.info("End of stream detected");
				break;
			}
			
			DAPRequest request = new DAPRequest(message);
			DAPMessageList responses = dispatcher.dispatch(request);
			
			if (responses != null)
			{
				for (JSONObject response: responses)
				{
					writeMessage(response);
				}
			}
		}

		Console.init(Charset.forName("UTF-8"));
	}
	
	public void stdout(String message)
	{
		try
		{
			writeMessage(new DAPResponse("output", new JSONObject("category", "stdout", "output", message)));
		}
		catch (IOException e)
		{
			Diag.error(e);
			Diag.info("%s", message);
		}
	}

	public void stderr(String message)
	{
		try
		{
			writeMessage(new DAPResponse("output", new JSONObject("category", "stderr", "output", message)));
		}
		catch (IOException e)
		{
			Diag.error(e);
			Diag.info("%s", message);
		}
	}

	public ConsoleWriter getOutConsoleWriter() throws IOException
	{
		return new DAPOutConsoleWriter(this);
	}

	public ConsoleWriter getErrConsoleWriter() throws IOException
	{
		return new DAPErrConsoleWriter(this);
	}
}
