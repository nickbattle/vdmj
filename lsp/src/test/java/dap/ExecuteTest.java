/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package dap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.RTLogger;

import dap.handlers.InitializeHandler;
import dap.handlers.LaunchHandler;
import json.JSONArray;
import json.JSONObject;
import lsp.CancellableThread;
import lsp.DAPTest;
import rpc.RPCMessageList;

public class ExecuteTest extends DAPTest
{
	@Test
	public void testLog() throws Exception
	{
		setupWorkspace(Dialect.VDM_RT);
		File testdir = new File("src/test/resources/cttest_rt");
		RPCMessageList notify = initialize(testdir, new JSONObject());
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);
		
		DAPHandler handler = new LaunchHandler();
		RTLogger.enable(false);
		
		File log = File.createTempFile("log", ".log");
		log.deleteOnExit();
		log.delete();
		assertEquals(false, log.exists());
		assertEquals(false, RTLogger.isEnabled());

		JSONObject args = new JSONObject(
				"noDebug", false,
				"defaultName", "A",
				"logging", log.getAbsolutePath());
		DAPRequest request = new DAPRequest(new JSONObject(
				"command", "launch",
				"type", "request",
				"arguments", args,
				"seq", 1));
		dump(request);
		
		DAPMessageList response = handler.run(request);
		assertEquals(1, response.size());
		assertEquals(true, response.get(0).get("success"));
		dump(response.get(0));
		
		handler = new InitializeHandler();
		request = new DAPRequest(new JSONObject(
				"command", "configurationDone",		// Creates log file from "logging"
				"type", "request",
				"arguments", null,
				"seq", 2));
		dump(request);
		
		response = handler.run(request);
		assertEquals(1, response.size());
		assertEquals(true, response.get(0).get("success"));
		dump(response.get(0));
		
		assertEquals(true, log.exists());
		assertEquals(true, RTLogger.isEnabled());
		log.delete();
		
		Thread background = CancellableThread.find("init");
		if (background != null) background.join();
	}
	
	@Test
	public void testUnknownCommand() throws Exception
	{
		System.setProperty("lspx.plugins", "plugins.AnotherPlugin");
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/pogtest_sl");
		RPCMessageList notify = initialize(testdir, new JSONObject());
		assertEquals(2, notify.size());

		dump(notify.get(0));
		assertEquals("textDocument/publishDiagnostics", notify.get(0).getPath("method"));
		assertTrue(notify.get(0).getPath("params.diagnostics") instanceof JSONArray);

		dap.UnknownHandler handler = new dap.UnknownHandler();

		DAPRequest request = new DAPRequest(new JSONObject("type", "request", "command", "unknown"));

		DAPMessageList response = handler.run(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		System.out.println("^^^");
		assertEquals("Unknown command: unknown", response.get(0).getPath("message"));
		assertEquals(false, response.get(0).getPath("success"));

		request = new DAPRequest(new JSONObject("type", "request", "command", "slsp/another"));

		response = handler.run(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		System.out.println("^^^");
		assertEquals("Plugin does not support analysis", response.get(0).getPath("message"));
		assertEquals(false, response.get(0).getPath("success"));
	}
}
