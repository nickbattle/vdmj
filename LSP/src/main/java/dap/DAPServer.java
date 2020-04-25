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
		
		dispatcher.register("initialize", new InitializeHandler(state));
		dispatcher.register("launch", new LaunchHandler(state));
		dispatcher.register("configurationDone", new InitializeHandler(state));
		dispatcher.register("threads", new ThreadsHandler(state));
		dispatcher.register("setBreakpoints", new SetBreakpointsHandler(state));
		dispatcher.register("evaluate", new EvaluateHandler(state));
		dispatcher.register("stackTrace", new StackTraceHandler(state));
		dispatcher.register("disconnect", new DisconnectHandler(state));
		dispatcher.register("terminate", new TerminateHandler(state));

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
}
