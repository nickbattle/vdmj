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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONArray;
import json.JSONObject;
import lsp.lspx.POGHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class POGTest extends LSPTest
{
	private JSONObject capabilities = new JSONObject(
			"experimental", new JSONObject("proofObligationGeneration", true));

	@Test
	public void testSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/pogtest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		dump(notify.get(1));
		assertEquals("slsp/POG/updated", notify.get(1).getPath("method"));
		assertEquals(true, notify.get(1).getPath("params.successful"));

		POGHandler handler = new POGHandler();
		File file = new File(testdir, "pogtest.vdmsl");
		RPCRequest request = RPCRequest.create(123L, "slsp/POG/generate",
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
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		dump(notify.get(1));
		assertEquals("slsp/POG/updated", notify.get(1).getPath("method"));
		assertEquals(true, notify.get(1).getPath("params.successful"));
		
		POGHandler handler = new POGHandler();
		RPCRequest request = RPCRequest.create(789L, "slsp/POG/generate",
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
	public void testRT() throws Exception
	{
		setupWorkspace(Dialect.VDM_RT);
		File testdir = new File("src/test/resources/pogtest_rt");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		dump(notify.get(1));
		assertEquals("slsp/POG/updated", notify.get(1).getPath("method"));
		assertEquals(true, notify.get(1).getPath("params.successful"));
		
		POGHandler handler = new POGHandler();
		RPCRequest request = RPCRequest.create(789L, "slsp/POG/generate",
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
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		Object array = notify.get(0).getPath("params.diagnostics");
		assertTrue(array instanceof JSONArray);
		JSONArray diags = (JSONArray)array;
		assertEquals(1, diags.size());
		assertEquals("Unable to resolve type name 'nt'", notify.get(0).getPath("params.diagnostics.[0].message"));
		
		dump(notify.get(1));
		assertEquals("slsp/POG/updated", notify.get(1).getPath("method"));
		assertEquals(false, notify.get(1).getPath("params.successful"));

		POGHandler handler = new POGHandler();
		RPCRequest request = RPCRequest.create(789L, "slsp/POG/generate",
				new JSONObject("uri", testdir.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("Specification errors found", response.get(0).getPath("error.message"));
	}	
}
