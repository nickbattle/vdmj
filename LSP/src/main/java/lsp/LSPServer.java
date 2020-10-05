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
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import json.JSONObject;
import json.JSONServer;
import lsp.lspx.POGHandler;
import lsp.textdocument.CompletionHandler;
import lsp.textdocument.DefinitionHandler;
import lsp.textdocument.DidChangeHandler;
import lsp.textdocument.DidCloseHandler;
import lsp.textdocument.DidOpenHandler;
import lsp.textdocument.DidSaveHandler;
import lsp.textdocument.DocumentSymbolHandler;
import lsp.workspace.DidChangeWSHandler;
import lsp.workspace.WorkspaceFoldersHandler;
import rpc.RPCDispatcher;
import rpc.RPCHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import vdmj.DAPDebugLink;
import workspace.Log;
import workspace.WorkspaceManager;

public class LSPServer extends JSONServer
{
	private static LSPServer INSTANCE = null;

	private final RPCDispatcher dispatcher;
	private final LSPServerState state;
	private final Map<Long, RPCHandler> responseHandlers;
	
	public LSPServer(Dialect dialect, InputStream inStream, OutputStream outStream) throws IOException
	{
		super("LSP", inStream, outStream);
		
		INSTANCE = this;
		this.state = new LSPServerState();
		this.dispatcher = getDispatcher();
		this.responseHandlers = new HashMap<Long, RPCHandler>();
		
		state.setManager(WorkspaceManager.createInstance(dialect));
		
		// Identify this class as the debug link - See DebugLink
		System.setProperty("vdmj.debug.link", DAPDebugLink.class.getName());
		Settings.annotations = true;
	}
	
	public static LSPServer getInstance()
	{
		return INSTANCE;
	}
	
	private RPCDispatcher getDispatcher() throws IOException
	{
		RPCDispatcher dispatcher = new RPCDispatcher();
		
		dispatcher.register(new InitializeHandler(state), "initialize", "initialized", "client/registerCapability");
		dispatcher.register("shutdown", new ShutdownHandler(state));
		dispatcher.register("exit", new ExitHandler(state));

		dispatcher.register("textDocument/didOpen", new DidOpenHandler(state));
		dispatcher.register("textDocument/didClose", new DidCloseHandler(state));
		dispatcher.register("textDocument/didChange", new DidChangeHandler(state));
		dispatcher.register("textDocument/didSave", new DidSaveHandler(state));
		dispatcher.register("textDocument/definition", new DefinitionHandler(state));
		dispatcher.register("textDocument/documentSymbol", new DocumentSymbolHandler(state));
		dispatcher.register("textDocument/completion", new CompletionHandler(state));

		dispatcher.register("workspace/didChangeWatchedFiles", new DidChangeWSHandler(state));
		dispatcher.register("workspace/workspaceFolders", new WorkspaceFoldersHandler(state));
		dispatcher.register("workspace/didChangeWorkspaceFolders", new DidChangeWSHandler(state));

		dispatcher.register(new POGHandler(state), "lspx/POG/generate", "lspx/POG/generate");

		return dispatcher;
	}
	
	@Override
	protected void setTimeout(int timeout) throws SocketException
	{
		// Ignored for stdio comms?
	}

	public void run() throws IOException
	{
		state.setRunning(true);
		responseHandlers.clear();
		
		while (state.isRunning())
		{
			JSONObject message = readMessage();
			
			if (message == null)	// EOF
			{
				Log.printf("End of stream detected");
				break;
			}
			
			if (message.get("method") == null && message.get("id") != null)		// A response
			{
				Long id = message.get("id");
				RPCHandler handler = responseHandlers.get(id);
				
				if (handler != null)
				{
					handler.response(new RPCResponse(message));
					responseHandlers.remove(id);
				}
				else
				{
					Log.error("Unhandled response, id=%d", id);
				}
			}
			else
			{
				RPCRequest request = new RPCRequest(message);
				RPCMessageList responses = dispatcher.dispatch(request);
				
				if (responses != null)
				{
					for (JSONObject response: responses)
					{
						writeMessage(response);
						
						if (response.get("method") != null && response.get("id") != null)	// A request
						{
							RPCRequest req = new RPCRequest(response);
							responseHandlers.put(response.get("id"), dispatcher.getHandler(req));
						}
					}
				}
			}
		}
	}
}
