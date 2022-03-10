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

package plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONArray;
import json.JSONObject;
import lsp.UnknownHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class TranslateTest extends LSPTest
{
	private JSONObject capabilities = new JSONObject(
			"experimental", new JSONObject("translate", true));
	
	@Before
	public void setUp()
	{
		System.clearProperty("lspx.plugins");
	}

	@Test
	public void testV2C() throws Exception
	{
		System.setProperty("lspx.plugins", "plugins.V2CPlugin");
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/v2ctest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);

		UnknownHandler handler = new UnknownHandler();
		File empty = Files.createTempDirectory("test").toFile();

		RPCRequest request = RPCRequest.create(123L, "slsp/v2c",
				new JSONObject(
					"uri", null,
					"languageId", "c",
					"saveUri",	empty.toURI().toString()));

		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(empty.toURI().toString(), response.get(0).getPath("result.uri"));

		for (File f: empty.listFiles())
		{
			assertTrue(f.getName().matches("^.*\\.c$"));
			f.delete();
		}
		
		empty.delete();
	}
}
