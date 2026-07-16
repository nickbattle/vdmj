/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
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
import lsp.lspx.OrderHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class OrderingTest extends LSPTest
{
	private JSONObject capabilities = new JSONObject("experimental", new JSONObject());

	@Test
	public void testSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/deptest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());
		
		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);

		OrderHandler handler = new OrderHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/ordering", null);
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("deptest.vdmsl", response.get(0).getPath("result.[0]"));
	}

	@Test
	public void testPP() throws Exception
	{
		setupWorkspace(Dialect.VDM_PP);
		File testdir = new File("src/test/resources/deptest_pp");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());
		
		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);

		OrderHandler handler = new OrderHandler();
		RPCRequest request = RPCRequest.create(123L, "slsp/ordering", null);
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("deptest.vdmpp", response.get(0).getPath("result.[0]"));
	}
}
