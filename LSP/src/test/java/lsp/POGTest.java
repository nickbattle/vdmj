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

import java.io.File;
import java.io.PrintWriter;

import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONArray;
import json.JSONObject;
import json.JSONWriter;
import lsp.lspx.POGHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.WorkspaceManager;

public class POGTest
{
	@Test
	public void testSL() throws Exception
	{
		WorkspaceManager.reset();
		WorkspaceManager manager = WorkspaceManager.createInstance(Dialect.VDM_SL);
		LSPServerState state = new LSPServerState();
		state.setManager(manager);
		
		File testdir = new File("src/test/resources/pogtest_sl");
		File file = new File(testdir, "pogtest.vdmsl");

		JSONObject params = new JSONObject("rootUri", testdir.toURI().toString());
		RPCRequest initRequest = new RPCRequest(0L, "initialize", params);
		manager.lspInitialize(initRequest);
		manager.afterChangeWatchedFiles(null);	// Cause parse and typecheck
		
		POGHandler handler = new POGHandler(state);
		JSONWriter writer = new JSONWriter(new PrintWriter(System.out));
		
		RPCRequest request = new RPCRequest(123L, "lspx/POG/generate",
				new JSONObject("uri", file.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		writer.writeObject(request);
		writer.writeObject(response.get(0));
		writer.flush();
		
		JSONArray ids = new JSONArray();
		JSONArray pos = response.get(0).get("result");
		
		for (int i=0; i<pos.size(); i++)
		{
			JSONObject po = pos.index(i);
			ids.add(po.get("id"));
		}
		
		RPCRequest request2 = new RPCRequest(456L, "lspx/POG/retrieve", new JSONObject("ids", ids));

		RPCMessageList response2 = handler.request(request2);
		writer.writeObject(request2);
		writer.writeObject(response2.get(0));
		writer.flush();
	}
	
	@Test
	public void testPP() throws Exception
	{
		WorkspaceManager.reset();
		WorkspaceManager manager = WorkspaceManager.createInstance(Dialect.VDM_PP);
		LSPServerState state = new LSPServerState();
		state.setManager(manager);
		
		File testdir = new File("src/test/resources/pogtest_pp");
		File file = new File(testdir, "pogtest.vdmpp");

		JSONObject params = new JSONObject("rootUri", testdir.toURI().toString());
		RPCRequest initRequest = new RPCRequest(0L, "initialize", params);
		manager.lspInitialize(initRequest);
		manager.afterChangeWatchedFiles(null);	// Cause parse and typecheck
		
		POGHandler handler = new POGHandler(state);
		JSONWriter writer = new JSONWriter(new PrintWriter(System.out));
		
		RPCRequest request = new RPCRequest(789L, "lspx/POG/generate",
				new JSONObject(
						"uri", file.toURI().toString(),
						"range", new JSONObject(
							"start", new JSONObject("line", 2L, "character", 0L),
							"end",   new JSONObject("line", 2L, "character", 100L))));
		
		RPCMessageList response = handler.request(request);
		writer.writeObject(request);
		writer.writeObject(response.get(0));
		writer.flush();
		
		JSONArray ids = new JSONArray();
		JSONArray pos = response.get(0).get("result");
		
		for (int i=0; i<pos.size(); i++)
		{
			JSONObject po = pos.index(i);
			ids.add(po.get("id"));
		}
		
		RPCRequest request2 = new RPCRequest(987L, "lspx/POG/retrieve", new JSONObject("ids", ids));

		RPCMessageList response2 = handler.request(request2);
		writer.writeObject(request2);
		writer.writeObject(response2.get(0));
		writer.flush();
	}
}
