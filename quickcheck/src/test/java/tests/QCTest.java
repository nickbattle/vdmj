/*******************************************************************************
 *
 *	Copyright (c) 2024 Nick Battle.
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

package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.fujitsu.vdmj.lex.Dialect;

import json.JSONArray;
import json.JSONObject;
import quickcheck.plugin.QuickCheckHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;

public class QCTest extends LSPTest
{
	private JSONObject capabilities = new JSONObject(
			"experimental", new JSONObject("proofObligationGeneration", true));
	@Test
	public void testQC() throws Exception
	{
		setupWorkspace(Dialect.VDM_SL);
		File testdir = new File("src/test/resources/qctest");
		RPCMessageList notify = initialize(testdir, capabilities);
		assertEquals(3, notify.size());
		
		dump(notify.get(0));
		assertEquals("slsp/POG/updated", notify.get(0).getPath("method"));
		assertEquals(true, notify.get(0).getPath("params.successful"));

		dump(notify.get(1));
		assertEquals("textDocument/publishDiagnostics", notify.get(1).getPath("method"));
		assertTrue(notify.get(1).getPath("params.diagnostics") instanceof JSONArray);

		QuickCheckHandler handler = new QuickCheckHandler();

		JSONObject params = new JSONObject
		(
			"config",
				new JSONObject
				(
					"timeout",		2L,
					"obligations",	new JSONArray(1)
				)
		);
		
		RPCRequest request = RPCRequest.create(123L, "slsp/POG/quickcheck", params);
		
		RPCMessageList response = handler.request(request);
		assertEquals(null, response);
	}
}
