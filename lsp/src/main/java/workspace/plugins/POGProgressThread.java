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

import java.io.IOException;

import com.fujitsu.vdmj.po.POProgress;

import json.JSONObject;
import lsp.CancellableThread;
import lsp.LSPServer;
import rpc.RPCRequest;
import workspace.Diag;

public class POGProgressThread extends CancellableThread
{
	private static final long POLL_INTERVAL = 200;	// fifth sec?
	private final POProgress progress;
	private final Object workDoneToken;

	public POGProgressThread(RPCRequest request, POProgress progress)
	{
		super("POG");

		this.progress = progress;
		JSONObject params = request.get("params");
		this.workDoneToken = params.get("workDoneToken");
	}

	@Override
	protected void body()
	{
		try
		{
			LSPServer server = LSPServer.getInstance();
			long percentDone = -1;
			int total = progress.getDefCount();

			if (workDoneToken != null)
			{
				int sofar = progress.getProgress();

				while (sofar < total)
				{
					long done = (100 * sofar)/total;
					
					if (done != percentDone)	// Only if changed %age
					{
						JSONObject value = null;
						
						if (percentDone < 0)	// First time
						{
							value = new JSONObject(
								"kind",			"begin",
								"title",		"Executing QuickCheck",
								"message",		"Processing QuickCheck",
								"percentage",	done);
						}
						else
						{
							value = new JSONObject(
								"kind",			"report",
								"message",		"Processing QuickCheck",
								"percentage",	done);
						}
						
						JSONObject params = new JSONObject("token", workDoneToken, "value", value);
						Diag.fine("Sending POG work done = %d%%", done);
						server.writeMessage(RPCRequest.notification("$/progress", params));
						percentDone = done;
					}

					sleep(POLL_INTERVAL);
					sofar = progress.getProgress();
				}
			}
		}
		catch (IOException e)
		{
			Diag.error(e);
		}
		catch (InterruptedException e)
		{
			Diag.fine("POG progress completed.");
		}
		finally
		{
			running = null;
		}
	}
}
