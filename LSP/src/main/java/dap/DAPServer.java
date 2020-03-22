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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import com.fujitsu.vdmj.lex.Dialect;

import dap.handlers.DisconnectHandler;
import dap.handlers.EvaluateHandler;
import dap.handlers.InitializeHandler;
import dap.handlers.LaunchHandler;
import dap.handlers.SetBreakpointsHandler;
import dap.handlers.TerminateHandler;
import dap.handlers.ThreadsHandler;
import json.JSONObject;
import json.JSONReader;
import json.JSONWriter;
import workspace.Log;

public class DAPServer
{
	private final InputStream inStream;
	private final OutputStream outStream;
	private final DAPServerState state;
	private final DAPDispatcher dispatcher;
	
	public DAPServer(Dialect dialect, InputStream inStream, OutputStream outStream) throws IOException
	{
		this.inStream = inStream;
		this.outStream = outStream;
		this.state = new DAPServerState(dialect);
		this.dispatcher = getDispatcher();
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
		
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
		String contentLength = br.readLine();
		br.readLine();	// blank separator
		
		while (state.isRunning() && contentLength != null)
		{
			int length = Integer.parseInt(contentLength.substring(16));	// Content-Length: NNNN
			char[] bytes = new char[length];
			int p = 0;
			
			for (int i=0; i<length; i++)
			{
				bytes[p++] = (char) br.read();
			}

			String message = new String(bytes);
			Log.printf(">>> %s", message);
			JSONReader jreader = new JSONReader(new StringReader(message));
			DAPRequest request = new DAPRequest(jreader.readObject());
			DAPMessageList responses = dispatcher.dispatch(request);
			
			if (responses != null)
			{
				for (JSONObject response: responses)
				{
					StringWriter swout = new StringWriter();
					JSONWriter jwriter = new JSONWriter(new PrintWriter(swout));
					jwriter.writeObject(response);
	
					String jout = swout.toString();
					Log.printf("<<< %s", jout);
					PrintWriter pwout = new PrintWriter(outStream);
					pwout.printf("Content-Length: %d\r\n\r\n%s", jout.length(), jout);
					pwout.flush();
				}
			}
				
			contentLength = br.readLine();
			br.readLine();	// blank separator
		}
	}
}
