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
			"experimental", new JSONObject("combinatorialTest", true));

	@Test
	public void testSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/cttest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(1, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		CTHandler handler = new CTHandler(state);
		RPCRequest request = new RPCRequest(123L, "lspx/CT/traces", new JSONObject());
		
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
}
