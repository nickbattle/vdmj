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

package workspace.plugins;

import java.io.File;
import java.io.IOException;

import com.fujitsu.vdmj.util.Progress;

import json.JSONArray;
import json.JSONObject;
import lsp.CancellableThread;
import lsp.LSPServer;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.MessageHub;
import workspace.PluginRegistry;

public class POGThread extends CancellableThread implements Progress
{
	private final RPCRequest request;
	private final File file;
	private final JSONArray obligations;
	private final LSPServer server = LSPServer.getInstance();

	public POGThread(RPCRequest request, File file, JSONArray obligations)
	{
		super(request.get("id"));		// So cancel kills this thread
		this.request = request;
		this.file = file;
		this.obligations = obligations;
	}

	@Override
	protected void body()
	{
		JSONObject params = request.get("params");
		workDoneToken = params.get("workDoneToken");
		
		POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
		total = pog.getTotal();
		pog.getProofObligations(this);

		RPCMessageList responses = pog.getPOGResponse(request, file, obligations);
		responses.addAll(MessageHub.getInstance().getDiagnosticResponses());

		for (JSONObject message: responses)
		{
			try
			{
				server.writeMessage(message);
			}
			catch (IOException e)
			{
				Diag.error(e);
			}
		}

		if (wasCancelled())
		{
			Diag.warning("POG was cancelled, clearing PO list");
			pog.obligationList = null;	// Force recalculation
		}

		running = null;
	}

	/**
	 * These methods and fields are updated by the PO*List getProofObligation methods.
	 */
	private int total;
	private String workDoneToken;
	private long percentDone = -1;
	private int progress = 0;

	@Override
	public void resetProgress()
	{
		progress = 0;
		percentDone = -1;	// Causes "begin" response
	}

	@Override
	public int getTotal()
	{
		return total;
	}

	@Override
	public int getProgress()
	{
		return progress;
	}

	@Override
	public void makeProgress(int n)
	{
		progress += n;

		if (progress <= total && !wasCancelled())
		{
			sendProgress();
		}
	}

	@Override
	public void cancelProgress()
	{
		setCancelled();
	}

	@Override
	public boolean cancelRequested()
	{
		return wasCancelled();
	}

	private void sendProgress()
	{
		try
		{
			long done = (100 * progress)/total;
			
			if (done != percentDone)	// Only send if changed %age
			{
				JSONObject value = null;
				
				if (percentDone < 0)	// First time
				{
					value = new JSONObject(
						"kind",			"begin",
						"title",		"Executing POG",
						"message",		"Processing POG",
						"percentage",	done);
				}
				else
				{
					value = new JSONObject(
						"kind",			"report",
						"message",		"Processing POG",
						"percentage",	done);
				}
				
				JSONObject params = new JSONObject("token", workDoneToken, "value", value);
				Diag.fine("Sending POG work done = %d%%", done);
				server.writeMessage(RPCRequest.notification("$/progress", params));
				percentDone = done;
			}
		}
		catch (IOException e)
		{
			Diag.error(e);
		}
	}
}
