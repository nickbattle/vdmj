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

package tests;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import json.JSONObject;
import json.JSONWriter;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.DAPWorkspaceManager;
import workspace.DAPXWorkspaceManager;
import workspace.Diag;
import workspace.LSPPlugin;

abstract public class LSPTest
{
	protected LSPPlugin lspManager = null;
	protected DAPWorkspaceManager dapManager = null;
	protected DAPXWorkspaceManager dapxManager = null;
	
	static
	{
		Diag.init(false);	// No logging, if lsp.log.level is unset
	}

	protected void setupWorkspace(Dialect dialect) throws IOException
	{
		Settings.dialect = dialect;
		LSPPlugin.reset();	// resets other managers, registry and hubs
		lspManager = LSPPlugin.getInstance();
		dapManager = DAPWorkspaceManager.getInstance();
		dapxManager = DAPXWorkspaceManager.getInstance();
	}
	
	protected RPCMessageList initialize(File root, JSONObject capabilities) throws Exception
	{
		RPCMessageList result = lspManager.lspInitialize(RPCRequest.create("initialize", null),
				new JSONObject(), root.getAbsoluteFile(), capabilities);
		assertEquals("init result", (Object)null, result.get(0).get("error"));		
		
		return lspManager.afterChangeWatchedFiles(null, 1, null);	// Cause parse and typecheck
	}
	
	protected void dump(JSONObject obj) throws IOException
	{
		PrintWriter pw = new PrintWriter(System.out);
		JSONWriter writer = new JSONWriter(pw);
		writer.writeObject(obj);
		pw.println();
		writer.flush();
	}
}
