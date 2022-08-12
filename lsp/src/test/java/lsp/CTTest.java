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
import lsp.lspx.CTHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class CTTest extends LSPTest
{
	private JSONObject capabilities = new JSONObject(
			"experimental", new JSONObject("combinatorialTesting", true));

	@Test
	public void testTracesSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/cttest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/traces", null);
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("A",     response.get(0).getPath("result.[0].name"));
		assertEquals("A`TA",  response.get(0).getPath("result.[0].traces.[0].name"));
		assertEquals("B",     response.get(0).getPath("result.[1].name"));
		assertEquals("B`TB1", response.get(0).getPath("result.[1].traces.[0].name"));
		assertEquals("B`TB2", response.get(0).getPath("result.[1].traces.[1].name"));
		assertEquals("B`TB3", response.get(0).getPath("result.[1].traces.[2].name"));
	}

	@Test
	public void testGenerateSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/cttest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "A`TA"));
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(Long.valueOf(25), response.get(0).getPath("result.numberOfTests"));

		request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "B`TB1"));
		response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(Long.valueOf(5), response.get(0).getPath("result.numberOfTests"));
	}

	@Test
	public void testExecuteSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/cttest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request =RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "A`TA"));
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		assertEquals(Long.valueOf(25), response.get(0).getPath("result.numberOfTests"));

		request = RPCRequest.create(123L, "slsp/CT/execute", new JSONObject(
				"name",					"A`TA",
				"range",				new JSONObject("start", 5, "end", 10),
				"partialResultToken",	999));
		
		response = handler.request(request);
		assertEquals(null, response);	// backgrounded
		
		Thread background = CancellableThread.find(123L);
		background.join();
	}

	@Test
	public void testExecuteAlarmSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/ctalarm_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "DEFAULT`Test1"));
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		assertEquals(Long.valueOf(160), response.get(0).getPath("result.numberOfTests"));

		request = RPCRequest.create(123L, "slsp/CT/execute", new JSONObject("name", "DEFAULT`Test1"));
		response = handler.request(request);
		assertEquals(null, response);	// backgrounded
		
		CancelHandler cancelHandler = new CancelHandler();
		request = RPCRequest.notification("$/cancelRequest", new JSONObject("id", 123L));
		response = cancelHandler.request(request);
		assertEquals(null, response);	// notify
		
		Thread background = CancellableThread.find(123L);
		background.join();
	}

	@Test
	public void testTracesPP() throws Exception
	{
		setupWorkspace(Dialect.VDM_PP);
		File testdir = new File("src/test/resources/cttest_pp");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/traces", new JSONObject());
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("A",     response.get(0).getPath("result.[0].name"));
		assertEquals("A`TA",  response.get(0).getPath("result.[0].traces.[0].name"));
		assertEquals("B",     response.get(0).getPath("result.[1].name"));
		assertEquals("B`TB1", response.get(0).getPath("result.[1].traces.[0].name"));
		assertEquals("B`TB2", response.get(0).getPath("result.[1].traces.[1].name"));
		assertEquals("B`TB3", response.get(0).getPath("result.[1].traces.[2].name"));
	}

	@Test
	public void testGeneratePP() throws Exception
	{
		setupWorkspace(Dialect.VDM_PP);
		File testdir = new File("src/test/resources/cttest_pp");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "A`TA"));
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(Long.valueOf(25), response.get(0).getPath("result.numberOfTests"));

		request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "B`TB1"));
		response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(Long.valueOf(5), response.get(0).getPath("result.numberOfTests"));
	}

	@Test
	public void testExecutePP() throws Exception
	{
		setupWorkspace(Dialect.VDM_PP);
		File testdir = new File("src/test/resources/cttest_pp");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "A`TA"));
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		assertEquals(Long.valueOf(25), response.get(0).getPath("result.numberOfTests"));

		request = RPCRequest.create(123L, "slsp/CT/execute", new JSONObject(
				"name",					"A`TA",
				"filter",				new JSONArray(
						new JSONObject("key", "trace reduction type", "value", "RANDOM"),
						new JSONObject("key", "subset limitation", "value", 10)),	// ie. 10%
				"partialResultToken",	999));

		response = handler.request(request);
		assertEquals(null, response);	// backgrounded
		
		Thread background = CancellableThread.find(123L);
		background.join();
	}

	@Test
	public void testTracesRT() throws Exception
	{
		setupWorkspace(Dialect.VDM_RT);
		File testdir = new File("src/test/resources/cttest_rt");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/traces", new JSONObject());
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("A",     response.get(0).getPath("result.[0].name"));
		assertEquals("A`TA",  response.get(0).getPath("result.[0].traces.[0].name"));
		assertEquals("B",     response.get(0).getPath("result.[1].name"));
		assertEquals("B`TB1", response.get(0).getPath("result.[1].traces.[0].name"));
		assertEquals("B`TB2", response.get(0).getPath("result.[1].traces.[1].name"));
		assertEquals("B`TB3", response.get(0).getPath("result.[1].traces.[2].name"));
	}

	@Test
	public void testGenerateRT() throws Exception
	{
		setupWorkspace(Dialect.VDM_RT);
		File testdir = new File("src/test/resources/cttest_rt");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "A`TA"));
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(Long.valueOf(25), response.get(0).getPath("result.numberOfTests"));

		request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "B`TB1"));
		response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(Long.valueOf(5), response.get(0).getPath("result.numberOfTests"));
	}

	@Test
	public void testExecuteRT() throws Exception
	{
		setupWorkspace(Dialect.VDM_RT);
		File testdir = new File("src/test/resources/cttest_rt");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/CT/generate", new JSONObject("name", "A`TA"));
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		assertEquals(Long.valueOf(25), response.get(0).getPath("result.numberOfTests"));

		request = RPCRequest.create(123L, "slsp/CT/execute", new JSONObject("name", "A`TA"));
		response = handler.request(request);
		assertEquals(null, response);	// backgrounded
		
		Thread background = CancellableThread.find(123L);
		background.join();
	}
}
