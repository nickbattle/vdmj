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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONObject;
import json.JSONServer;
import lsp.textdocument.DefinitionHandler;
import lsp.textdocument.DidChangeHandler;
import lsp.textdocument.DidCloseHandler;
import lsp.textdocument.DidOpenHandler;
import lsp.textdocument.DidSaveHandler;
import lsp.textdocument.DocumentSymbolHandler;
import rpc.RPCDispatcher;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.DAPDebugLink;
import workspace.Log;
import workspace.WorkspaceManager;

public class LSPServer extends JSONServer
{
	private final RPCDispatcher dispatcher;
	private final LSPServerState state;
	
	public LSPServer(Dialect dialect, InputStream inStream, OutputStream outStream) throws IOException
	{
		super("LSP", inStream, outStream);
		
		this.state = new LSPServerState();
		this.dispatcher = getDispatcher();

		state.setManager(WorkspaceManager.getInstance(dialect));
		
		// Identify this class as the debug link - See DebugLink
		System.setProperty("vdmj.debug.link", DAPDebugLink.class.getName());
	}
	
	private RPCDispatcher getDispatcher() throws IOException
	{
		RPCDispatcher dispatcher = new RPCDispatcher();
		
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
		state.setRunning(true);
		
		while (state.isRunning())
		{
			JSONObject message = readMessage();
			
			if (message == null)	// EOF
			{
				Log.printf("End of stream detected");
				break;
			}

			RPCRequest request = new RPCRequest(message);
			RPCMessageList responses = dispatcher.dispatch(request);
			
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
