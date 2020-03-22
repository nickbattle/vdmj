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

package lsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONObject;
import json.JSONReader;
import json.JSONWriter;
import lsp.textdocument.DefinitionHandler;
import lsp.textdocument.DidChangeHandler;
import lsp.textdocument.DidCloseHandler;
import lsp.textdocument.DidOpenHandler;
import lsp.textdocument.DidSaveHandler;
import lsp.textdocument.DocumentSymbolHandler;
import rpc.RPCDispatcher;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.WorkspaceManager;

public class LSPServer
{
	private final Dialect dialect;
	private final InputStream inStream;
	private final OutputStream outStream;
	private final RPCDispatcher dispatcher;
	
	public LSPServer(Dialect dialect, InputStream inStream, OutputStream outStream) throws IOException
	{
		this.dialect = dialect;
		this.inStream = inStream;
		this.outStream = outStream;
		this.dispatcher = getDispatcher();
	}
	
	private RPCDispatcher getDispatcher() throws IOException
	{
		RPCDispatcher dispatcher = new RPCDispatcher();
		LSPServerState state = new LSPServerState();
		state.setManager(WorkspaceManager.getInstance(dialect));
		
		dispatcher.register("initialize", new InitializeHandler(state));
		dispatcher.register("initialized", new InitializeHandler(state));
		dispatcher.register("shutdown", new ShutdownHandler(state));
		dispatcher.register("exit", new ExitHandler(state));

		dispatcher.register("textDocument/didOpen", new DidOpenHandler(state));
		dispatcher.register("textDocument/didClose", new DidCloseHandler(state));
		dispatcher.register("textDocument/didChange", new DidChangeHandler(state));
		dispatcher.register("textDocument/didSave", new DidSaveHandler(state));
		dispatcher.register("textDocument/definition", new DefinitionHandler(state));
		dispatcher.register("textDocument/documentSymbol", new DocumentSymbolHandler(state));

		return dispatcher;
	}

	public void run() throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
		String contentLength = br.readLine();
		br.readLine();	// blank separator
		
		while (contentLength != null)
		{
			int length = Integer.parseInt(contentLength.substring(16));	// Content-Length: NNNN
			char[] bytes = new char[length];
			int p = 0;
			
			for (int i=0; i<length; i++)
			{
				bytes[p++] = (char) br.read();
			}

			String message = new String(bytes);
			JSONReader jreader = new JSONReader(new StringReader(message));
			RPCRequest request = new RPCRequest(jreader.readObject());
			RPCMessageList responses = dispatcher.dispatch(request);
			
			if (responses != null)
			{
				for (JSONObject response: responses)
				{
					StringWriter swout = new StringWriter();
					JSONWriter jwriter = new JSONWriter(new PrintWriter(swout));
					jwriter.writeObject(response);
	
					String jout = swout.toString();
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
