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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintWriter;

import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;

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

		JSONObject params = new JSONObject(
				"rootUri",		testdir.toURI().toString(),
				"capabilities",	new JSONObject(
					"experimental", new JSONObject("proofObligationGeneration", true)));
		RPCRequest initRequest = new RPCRequest(0L, "initialize", params);
		manager.lspInitialize(initRequest);
		RPCMessageList notify = manager.afterChangeWatchedFiles(null);	// Cause parse and typecheck

		JSONWriter writer = new JSONWriter(new PrintWriter(System.out));
		assertEquals(notify.size(), 2);
		writer.writeObject(notify.get(0));
		writer.writeObject(notify.get(1));
		
		POGHandler handler = new POGHandler(state);
		
		RPCRequest request = new RPCRequest(123L, "lspx/POG/generate",
				new JSONObject("uri", file.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		assertEquals(response.size(), 1);
		writer.writeObject(request);
		writer.writeObject(response.get(0));
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

		JSONObject params = new JSONObject(
				"rootUri",		testdir.toURI().toString(),
				"capabilities",	new JSONObject(
					"experimental", new JSONObject("proofObligationGeneration", true)));
		RPCRequest initRequest = new RPCRequest(0L, "initialize", params);
		manager.lspInitialize(initRequest);
		RPCMessageList notify = manager.afterChangeWatchedFiles(null);	// Cause parse and typecheck

		JSONWriter writer = new JSONWriter(new PrintWriter(System.out));
		assertEquals(notify.size(), 2);
		writer.writeObject(notify.get(0));
		writer.writeObject(notify.get(1));
		
		POGHandler handler = new POGHandler(state);
		
		RPCRequest request = new RPCRequest(789L, "lspx/POG/generate",
				new JSONObject("uri", file.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		writer.writeObject(request);
		writer.writeObject(response.get(0));
		writer.flush();
	}
}
