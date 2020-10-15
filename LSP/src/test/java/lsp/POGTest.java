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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
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
	private WorkspaceManager manager = null;
	private LSPServerState state = null;

	private void setupWorkspace(Dialect dialect) throws IOException
	{
		WorkspaceManager.reset();
		manager = WorkspaceManager.createInstance(dialect);
		state = new LSPServerState();
		state.setManager(manager);
	}
	
	private RPCMessageList initialize(File root) throws Exception
	{
		JSONObject params = new JSONObject(
				"rootUri",		root.toURI().toString(),
				"capabilities",	new JSONObject(
					"experimental", new JSONObject(
						"proofObligationGeneration", true)));
		
		RPCMessageList result = manager.lspInitialize(new RPCRequest(0L, "initialize", params));
		assertEquals("init result", (Object)null, result.get(0).get("error"));		
		
		return manager.afterChangeWatchedFiles(null);	// Cause parse and typecheck
	}
	
	private void dump(JSONObject obj) throws IOException
	{
		PrintWriter pw = new PrintWriter(System.out);
		JSONWriter writer = new JSONWriter(pw);
		writer.writeObject(obj);
		pw.println();
		writer.flush();
	}
	
	@Test
	public void testSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/pogtest_sl");
		RPCMessageList notify = initialize(testdir);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		dump(notify.get(1));
		assertEquals("lspx/POG/updated", notify.get(1).getPath("method"));
		assertEquals(true, notify.get(1).getPath("params.successful"));

		POGHandler handler = new POGHandler(state);
		File file = new File(testdir, "pogtest.vdmsl");
		RPCRequest request = new RPCRequest(123L, "lspx/POG/generate",
				new JSONObject("uri", file.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("non-zero", response.get(0).getPath("result.[0].kind"));
		assertEquals("total function", response.get(0).getPath("result.[1].kind"));
		assertEquals("non-zero", response.get(0).getPath("result.[2].kind"));
		assertEquals("recursive function", response.get(0).getPath("result.[3].kind"));
		assertEquals("subtype", response.get(0).getPath("result.[4].kind"));
	}
	
	@Test
	public void testPP() throws Exception
	{
		setupWorkspace(Dialect.VDM_PP);
		File testdir = new File("src/test/resources/pogtest_pp");
		RPCMessageList notify = initialize(testdir);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		dump(notify.get(1));
		assertEquals("lspx/POG/updated", notify.get(1).getPath("method"));
		assertEquals(true, notify.get(1).getPath("params.successful"));
		
		POGHandler handler = new POGHandler(state);
		RPCRequest request = new RPCRequest(789L, "lspx/POG/generate",
				new JSONObject("uri", testdir.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("non-zero", response.get(0).getPath("result.[0].kind"));
		assertEquals("total function", response.get(0).getPath("result.[1].kind"));
		assertEquals("non-zero", response.get(0).getPath("result.[2].kind"));
		assertEquals("recursive function", response.get(0).getPath("result.[3].kind"));
		assertEquals("subtype", response.get(0).getPath("result.[4].kind"));
	}
	
	@Test
	public void testSLErrors() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/pogerrors_sl");
		RPCMessageList notify = initialize(testdir);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		Object array = notify.get(0).getPath("params.diagnostics");
		assertTrue(array instanceof JSONArray);
		JSONArray diags = (JSONArray)array;
		assertEquals(1, diags.size());
		assertEquals("Unable to resolve type name 'nt'", notify.get(0).getPath("params.diagnostics.[0].message"));
		
		dump(notify.get(1));
		assertEquals("lspx/POG/updated", notify.get(1).getPath("method"));
		assertEquals(false, notify.get(1).getPath("params.successful"));

		POGHandler handler = new POGHandler(state);
		RPCRequest request = new RPCRequest(789L, "lspx/POG/generate",
				new JSONObject("uri", testdir.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("Type checking errors found", response.get(0).getPath("error.message"));
	}	
}
