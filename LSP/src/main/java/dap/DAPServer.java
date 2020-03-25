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
import java.io.InputStream;
import java.io.OutputStream;
import com.fujitsu.vdmj.lex.Dialect;

import dap.handlers.DisconnectHandler;
import dap.handlers.EvaluateHandler;
import dap.handlers.InitializeHandler;
import dap.handlers.LaunchHandler;
import dap.handlers.SetBreakpointsHandler;
import dap.handlers.TerminateHandler;
import dap.handlers.ThreadsHandler;
import json.JSONObject;
import json.JSONServer;

public class DAPServer extends JSONServer
{
	private static DAPServer INSTANCE = null;
	
	private final DAPServerState state;
	private final DAPDispatcher dispatcher;
	
	public DAPServer(Dialect dialect, InputStream inStream, OutputStream outStream) throws IOException
	{
		super("DAP", inStream, outStream);
		
		INSTANCE = this;
		this.state = new DAPServerState(dialect);
		this.dispatcher = getDispatcher();
	}
	
	public static DAPServer getInstance()
	{
		return INSTANCE;
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
		dispatcher.register("disconnect", new DisconnectHandler(state));
		dispatcher.register("terminate", new TerminateHandler(state));

		return dispatcher;
	}

	public void run() throws IOException
	{
		state.setRunning(true);
		
		while (state.isRunning())
		{
			JSONObject message = readMessage();
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
}
