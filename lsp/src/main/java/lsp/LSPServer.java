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

package lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import json.JSONObject;
import json.JSONServer;
import lsp.lspx.CTHandler;
import lsp.lspx.POGHandler;
import lsp.lspx.TranslateHandler;
import lsp.textdocument.CompletionHandler;
import lsp.textdocument.DefinitionHandler;
import lsp.textdocument.DidChangeHandler;
import lsp.textdocument.DidCloseHandler;
import lsp.textdocument.DidOpenHandler;
import lsp.textdocument.DidSaveHandler;
import lsp.textdocument.DocumentSymbolHandler;
import lsp.workspace.DidChangeWSHandler;
import rpc.RPCDispatcher;
import rpc.RPCHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import vdmj.DAPDebugLink;
import workspace.LSPXWorkspaceManager;
import workspace.Log;

public class LSPServer extends JSONServer
{
	private static LSPServer INSTANCE = null;

	private final RPCDispatcher dispatcher;
	private final Map<Long, RPCHandler> responseHandlers;
	private boolean initialized = false;
	
	public LSPServer(Dialect dialect, InputStream inStream, OutputStream outStream) throws IOException
	{
		super("LSP", inStream, outStream);
		
		INSTANCE = this;
		this.dispatcher = getDispatcher();
		this.responseHandlers = new HashMap<Long, RPCHandler>();

		// Identify this class as the debug link - See DebugLink
		System.setProperty("vdmj.debug.link_class", DAPDebugLink.class.getName());
		Settings.annotations = true;
		Settings.dialect = dialect;

		LSPXWorkspaceManager.getInstance();		// Just set up
	}
	
	public static LSPServer getInstance()
	{
		return INSTANCE;
	}
	
	private RPCDispatcher getDispatcher() throws IOException
	{
		RPCDispatcher dispatcher = new RPCDispatcher();
		
		dispatcher.register(new InitializeHandler(), "initialize", "initialized", "client/registerCapability");
		dispatcher.register(new ShutdownHandler(), "shutdown");
		dispatcher.register(new ExitHandler(), "exit");
		dispatcher.register(new CancelHandler(), "$/cancelRequest");

		dispatcher.register(new DidOpenHandler(), "textDocument/didOpen");
		dispatcher.register(new DidCloseHandler(), "textDocument/didClose");
		dispatcher.register(new DidChangeHandler(), "textDocument/didChange");
		dispatcher.register(new DidSaveHandler(), "textDocument/didSave");
		dispatcher.register(new DefinitionHandler(), "textDocument/definition");
		dispatcher.register(new DocumentSymbolHandler(), "textDocument/documentSymbol");
		dispatcher.register(new CompletionHandler(), "textDocument/completion");

		dispatcher.register(new DidChangeWSHandler(), "workspace/didChangeWatchedFiles");

		dispatcher.register(new POGHandler(), "slsp/POG/generate");
		dispatcher.register(new CTHandler(), "slsp/CT/traces", "slsp/CT/generate", "slsp/CT/execute");
		dispatcher.register(new TranslateHandler(), "slsp/TR/translate");

		return dispatcher;
	}
	
	public void run() throws IOException
	{
		boolean running = true;
		responseHandlers.clear();
		
		while (running)
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
					handler.response(RPCResponse.create(message));
					responseHandlers.remove(id);
				}
				else
				{
					Log.error("Unhandled response, id=%d", id);
				}
			}
			else
			{
				RPCRequest request = RPCRequest.create(message);
				RPCMessageList responses = dispatcher.dispatch(request);
				
				if (responses != null)
				{
					for (JSONObject response: responses)
					{
						writeMessage(response);
						
						if (response.get("method") != null && response.get("id") != null)	// A request
						{
							RPCRequest req = RPCRequest.create(response);
							responseHandlers.put(response.get("id"), dispatcher.getHandler(req));
						}
					}
				}
			}
		}
	}

	public boolean isInitialized()
	{
		return initialized;
	}
	
	public void setInitialized(boolean set)
	{
		initialized = set;
	}
}
