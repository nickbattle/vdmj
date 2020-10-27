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
 *
 ******************************************************************************/

package dap;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.ConsoleWriter;

import dap.handlers.DisconnectHandler;
import dap.handlers.EvaluateHandler;
import dap.handlers.InitializeHandler;
import dap.handlers.LaunchHandler;
import dap.handlers.SetBreakpointsHandler;
import dap.handlers.StackTraceHandler;
import dap.handlers.TerminateHandler;
import dap.handlers.ThreadsHandler;
import json.JSONObject;
import json.JSONServer;
import workspace.DAPWorkspaceManager;
import workspace.Log;

public class DAPServer extends JSONServer
{
	private static DAPServer INSTANCE = null;
	
	private final DAPServerState state;
	private final DAPDispatcher dispatcher;
	private final Socket socket;
	
	public DAPServer(Dialect dialect, Socket socket) throws IOException
	{
		super("DAP", socket.getInputStream(), socket.getOutputStream());
		
		INSTANCE = this;
		this.state = new DAPServerState(dialect);
		this.dispatcher = getDispatcher();
		this.socket = socket;
		
		DAPWorkspaceManager.getInstance();		// Just set up
	}
	
	public static DAPServer getInstance()
	{
		return INSTANCE;
	}
	
	public DAPServerState getState()
	{
		return state;
	}
	
	private DAPDispatcher getDispatcher() throws IOException
	{
		DAPDispatcher dispatcher = new DAPDispatcher();
		
		dispatcher.register(new InitializeHandler(state), "initialize");
		dispatcher.register(new LaunchHandler(state), "launch");
		dispatcher.register(new InitializeHandler(state), "configurationDone");
		dispatcher.register(new ThreadsHandler(state), "threads");
		dispatcher.register(new SetBreakpointsHandler(state), "setBreakpoints");
		dispatcher.register(new EvaluateHandler(state), "evaluate");
		dispatcher.register(new StackTraceHandler(state), "stackTrace");
		dispatcher.register(new DisconnectHandler(state), "disconnect");
		dispatcher.register(new TerminateHandler(state), "terminate");

		return dispatcher;
	}

	@Override
	protected void setTimeout(int timeout) throws SocketException
	{
		socket.setSoTimeout(timeout);
	}

	public void run() throws IOException
	{
		state.setRunning(true);
		Console.init("UTF-8", getOutConsoleWriter(), getErrConsoleWriter());

		while (state.isRunning())
		{
			JSONObject message = readMessage();
			
			if (message == null)	// EOF
			{
				Log.printf("End of stream detected");
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

		Console.init("UTF-8");
	}
	
	public void stdout(String message)
	{
		try
		{
			writeMessage(new DAPResponse("output", new JSONObject("category", "stdout", "output", message)));
		}
		catch (IOException e)
		{
			Log.error(e);
			Log.printf("%s", message);
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
			Log.error(e);
			Log.printf("%s", message);
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
