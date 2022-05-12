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

package lsp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.RTLogger;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.handlers.LogHandler;
import json.JSONArray;
import json.JSONObject;
import rpc.RPCMessageList;

public class LogTest extends DAPTest
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
		
		LogHandler handler = new LogHandler();
		RTLogger.enable(false);
		
		File log = File.createTempFile("log", ".log");
		log.deleteOnExit();
		DAPRequest request = new DAPRequest(new JSONObject("command", "sdap/log", "type", "request", "arguments", log.getAbsolutePath(), "seq", 1));
		dump(request);
		
		DAPMessageList response = handler.run(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals("RT events now logged to " + log.getAbsolutePath(), response.get(0).getPath("body.result"));
		assertTrue(RTLogger.isEnabled());
		
		request = new DAPRequest(new JSONObject("command", "sdap/log", "type", "request", "seq", 2));
		dump(request);
		
		RTLogger.enable(false);
		response = handler.run(request);
		assertEquals(1, response.size());
		dump(response.get(0));
		assertEquals("RT event logging disabled", response.get(0).getPath("body.result"));
		assertTrue(!RTLogger.isEnabled());
	}
}
