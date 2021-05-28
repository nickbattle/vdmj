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
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONArray;
import json.JSONObject;
import lsp.lspx.TranslateHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class TranslateTest extends LSPTest
{
	private JSONObject capabilities = new JSONObject(
			"experimental", new JSONObject("translate", true));

	@Test
	public void testSLErrors() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/pogtest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(1, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);

		TranslateHandler handler = new TranslateHandler();
		File file = new File(testdir, "pogtest.vdmsl");

		RPCRequest request = RPCRequest.create(123L, "slsp/TR/translate",
				new JSONObject(
					"uri", file.toURI().toString(),
					"languageId", "latex",
					"saveUri",	testdir.toURI().toString()));
		
		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("saveUri is not empty", response.get(0).getPath("error.message"));
		assertEquals(new Long(-32602), response.get(0).getPath("error.code"));

		request = RPCRequest.create(123L, "slsp/TR/translate",
				new JSONObject(
					"uri", file.toURI().toString(),
					"languageId", "latex",
					"saveUri",	file.toURI().toString()));
		
		response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("saveUri is not a folder", response.get(0).getPath("error.message"));
		assertEquals(new Long(-32602), response.get(0).getPath("error.code"));

		request = RPCRequest.create(123L, "slsp/TR/translate",
				new JSONObject(
					"uri", file.toURI().toString(),
					"languageId", "latex",
					"saveUri",	new File("???!!!").toURI().toString()));
		
		response = handler.request(request);
		assertEquals(1, response.size());

		dump(response.get(0));
		assertEquals("saveUri does not exist", response.get(0).getPath("error.message"));
		assertEquals(new Long(-32602), response.get(0).getPath("error.code"));
		
		Path empty = Files.createTempDirectory("test");

		request = RPCRequest.create(123L, "slsp/TR/translate",
				new JSONObject(
					"uri", file.toURI().toString(),
					"languageId", "Chinese",
					"saveUri",	empty.toUri().toString()));
		
		response = handler.request(request);
		assertEquals(1, response.size());
		empty.toFile().delete();

		dump(response.get(0));
		assertEquals("Unsupported language", response.get(0).getPath("error.message"));
		assertEquals(new Long(-32602), response.get(0).getPath("error.code"));
	}

	@Test
	public void testSL() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/pogtest_sl");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(1, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);

		TranslateHandler handler = new TranslateHandler();
		File empty = Files.createTempDirectory("test").toFile();

		RPCRequest request = RPCRequest.create(123L, "slsp/TR/translate",
				new JSONObject(
					"uri", null,
					"languageId", "latex",
					"saveUri",	empty.toURI().toString()));

		RPCMessageList response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(empty.toURI().toString(), response.get(0).getPath("result.uri"));

		for (File f: empty.listFiles())
		{
			assertTrue(f.getName().matches("^.*\\.tex$"));
			f.delete();
		}
		
		File file = new File(testdir, "pogtest.vdmsl");
		request = RPCRequest.create(123L, "slsp/TR/translate",
				new JSONObject(
					"uri", file.toURI().toString(),
					"languageId", "latex",
					"saveUri",	empty.toURI().toString()));

		response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(file.toURI().toString(), response.get(0).getPath("result.uri"));

		for (File f: empty.listFiles())
		{
			assertTrue(f.getName().matches("^.*\\.tex$"));
			f.delete();
		}
		
		File coverage = new File(testdir, "pogtest.vdmsl");
		request = RPCRequest.create(123L, "slsp/TR/translate",
				new JSONObject(
					"uri", coverage.toURI().toString(),
					"languageId", "coverage",
					"saveUri",	empty.toURI().toString()));

		response = handler.request(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals(empty.toURI()+"pogtest.vdmsl.covtbl", response.get(0).getPath("result.uri"));

		for (File f: empty.listFiles())
		{
			assertTrue(f.getName().matches("^.*\\.vdmsl\\.covtbl$"));
			f.delete();
		}
		
		empty.delete();
	}
}
