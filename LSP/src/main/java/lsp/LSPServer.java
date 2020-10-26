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
import workspace.LSPWorkspaceManager;
import workspace.LSPXWorkspaceManager;
import workspace.Log;

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

		// Identify this class as the debug link - See DebugLink
		System.setProperty("vdmj.debug.link", DAPDebugLink.class.getName());
		Settings.annotations = true;
		Settings.dialect = dialect;

		LSPWorkspaceManager.getInstance().setLSPState(state);
		LSPXWorkspaceManager.getInstance();		// Just set up
	}
	
	public static LSPServer getInstance()
	{
		return INSTANCE;
	}
	
	private RPCDispatcher getDispatcher() throws IOException
	{
		RPCDispatcher dispatcher = new RPCDispatcher();
		
		dispatcher.register(new InitializeHandler(state), "initialize", "initialized", "client/registerCapability");
		dispatcher.register(new ShutdownHandler(state), "shutdown");
		dispatcher.register(new ExitHandler(state), "exit");

		dispatcher.register(new DidOpenHandler(state), "textDocument/didOpen");
		dispatcher.register(new DidCloseHandler(state), "textDocument/didClose");
		dispatcher.register(new DidChangeHandler(state), "textDocument/didChange");
		dispatcher.register(new DidSaveHandler(state), "textDocument/didSave");
		dispatcher.register(new DefinitionHandler(state), "textDocument/definition");
		dispatcher.register(new DocumentSymbolHandler(state), "textDocument/documentSymbol");
		dispatcher.register(new CompletionHandler(state), "textDocument/completion");

		dispatcher.register(new DidChangeWSHandler(state), "workspace/didChangeWatchedFiles");
		dispatcher.register(new WorkspaceFoldersHandler(state), "workspace/workspaceFolders");
		dispatcher.register(new DidChangeWSHandler(state), "workspace/didChangeWorkspaceFolders");

		dispatcher.register(new POGHandler(state), "lspx/POG/generate", "lspx/POG/retrieve");

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
