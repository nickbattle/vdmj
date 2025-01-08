/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.plugin;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.debug.ConsoleExecTimer;
import com.fujitsu.vdmj.messages.VDMMessage;
import com.fujitsu.vdmj.messages.VDMWarning;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.RecursiveObligation;

import json.JSONArray;
import json.JSONObject;
import lsp.CancellableThread;
import lsp.LSPServer;
import lsp.Utils;
import quickcheck.QuickCheck;
import quickcheck.strategies.StrategyResults;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import rpc.RPCResponse;
import workspace.Diag;
import workspace.MessageHub;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

public class QuickCheckThread extends CancellableThread
{
	private final RPCRequest request;
	private final QuickCheck qc;
	private final ProofObligationList chosen;
	private final POPlugin pog;
	private final Object workDoneToken;
	private final long timeout;

	public QuickCheckThread(RPCRequest request, QuickCheck qc, ProofObligationList chosen, long timeout)
	{
		super(request.get("id"));
		this.request = request;
		this.qc = qc;
		this.chosen = chosen;
		this.pog = PluginRegistry.getInstance().getPlugin("PO");
		this.timeout = timeout;
		
		JSONObject params = request.get("params");
		this.workDoneToken = params.get("workDoneToken");
	}

	@Override
	protected void body()
	{
		try
		{
			running = "quickcheck";
			RPCMessageList responses = new RPCMessageList();
			
			LSPServer server = LSPServer.getInstance();
			MessageHub.getInstance().clearPluginMessages(pog);
			pog.clearLenses();
			
			List<VDMMessage> messages = new Vector<VDMMessage>();
			JSONArray list = new JSONArray();
			long percentDone = -1;
			int count = 0;
			
			for (ProofObligation po: chosen)
			{
				StrategyResults results = qc.getValues(po);
				
				if (!qc.hasErrors())
				{
					ConsoleExecTimer execTimer = null;
					
					try
					{
						execTimer = new ConsoleExecTimer(timeout);
						execTimer.start();

						qc.checkObligation(po, results);
					}
					finally
					{
						if (execTimer != null)
						{
							execTimer.interrupt();
						}
					}
				}
				
				list.add(getQCResponse(po, messages));
				count++;
				
				if (workDoneToken != null)
				{
					long done = (100 * count)/chosen.size();
					
					if (done != percentDone)	// Only if changed %age
					{
						JSONObject value = null;
						
						if (percentDone < 0)
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
						Diag.fine("Sending QC work done = %d%%", done);
						server.writeMessage(RPCRequest.notification("$/progress", params));
						percentDone = done;
					}
				}
				
				if (cancelled)
				{
					list.clear();
					break;
				}
			}

			responses.add(RPCResponse.result(request, list));

			if (!cancelled)
			{
				MessageHub.getInstance().addPluginMessages(pog, messages);
				responses.addAll(MessageHub.getInstance().getDiagnosticResponses());
				responses.add(RPCRequest.create("workspace/codeLens/refresh", null));
			}
			
			for (JSONObject message: responses)
			{
				server.writeMessage(message);
			}
		}
		catch (IOException e)
		{
			Diag.error(e);
		}
		finally
		{
			running = null;
		}
	}

	private JSONObject getQCResponse(ProofObligation po, List<VDMMessage> messages)
	{
		JSONObject json = new JSONObject(
				"id",		Long.valueOf(po.number),
				"status",	po.status.toString());
		
		if (po.counterexample != null)
		{
			JSONObject cexample = new JSONObject();
			cexample.put("variables", Utils.contextToJSON(po.counterexample));
			JSONObject launch = pog.getCexLaunch(po);
			
			if (po instanceof RecursiveObligation)
			{
				RecursiveObligation rec = (RecursiveObligation)po;
				
				if (rec.mutuallyRecursive)
				{
					// Recursive function obligations check the measure_f value for each
					// (mutually) recursive call. So a launch would have to make two comparisons
					// of measure values. Until we can figure out how to do this, we don't
					// send a launch string, but set a message to display instead.
					
					json.put("message", "Mutually recursive measures fail for these bindings");
					launch = null;
				}
			}
			
			if (launch != null)
			{
				cexample.put("launch", launch);
				pog.addCodeLens(po);
			}
			
			json.put("counterexample", cexample);
			
			StringBuilder sb = new StringBuilder();
			sb.append("PO #");
			sb.append(po.number);
			sb.append(" Counterexample: ");
			sb.append(po.counterexample.toStringLine());
			messages.add(new VDMWarning(9000, sb.toString(), po.location));
		}

		if (po.status == POStatus.FAILED || po.status == POStatus.MAYBE)
		{
			if (po.message != null)		// Add failed messages/qualifiers as a warning too
			{
				StringBuilder sb = new StringBuilder();
				sb.append("PO #");
				sb.append(po.number);
				sb.append(" ");
				sb.append(po.message);
				messages.add(new VDMWarning(9000, sb.toString(), po.location));
			}
			else if (po.qualifier != null)
			{
				StringBuilder sb = new StringBuilder();
				sb.append("PO #");
				sb.append(po.number);
				sb.append(" ");
				sb.append(po.qualifier);
				messages.add(new VDMWarning(9000, sb.toString(), po.location));
			}
		}
		
		if (po.witness != null)
		{
			JSONObject witness = new JSONObject();
			witness.put("variables", Utils.contextToJSON(po.witness));
			JSONObject launch = pog.getWitnessLaunch(po);
			
			if (launch != null)
			{
				witness.put("launch", launch);
			}
			
			json.put("witness", witness);
		}
		
		if (po.provedBy != null)
		{
			json.put("provedBy", po.provedBy);
		}
		
		if (po.message != null)
		{
			json.put("message", po.message);
		}

		return json;
	}
}
