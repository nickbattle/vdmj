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

package quickcheck.commands;

import java.io.IOException;
import java.util.List;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.debug.ConsoleExecTimer;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import dap.AsyncExecutor;
import dap.DAPRequest;
import dap.DAPResponse;
import json.JSONObject;
import lsp.LSPServer;
import quickcheck.QuickCheck;
import quickcheck.strategies.StrategyResults;
import rpc.RPCRequest;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

public class QuickCheckExecutor extends AsyncExecutor
{
	private final QuickCheck qc;
	private final long timeout;
	private final List<Integer> poList;
	private final List<String> poNames;
	private final boolean nominal;
	private String answer;

	public QuickCheckExecutor(DAPRequest request, QuickCheck qc,
			long timeout, List<Integer> poList, List<String> poNames, boolean nominal)
	{
		super("qc", request);
		this.qc = qc;
		this.timeout = timeout;
		this.poList = poList;
		this.poNames = poNames;
		this.nominal = nominal;
	}

	@Override
	protected void head()
	{
		running = "qc";
	}

	@Override
	protected void exec() throws Exception
	{
		POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = pog.getProofObligations();
		ProofObligationList chosen = qc.getPOs(all, poList, poNames);
		List<POStatus> includes = QCConsole.getIncludes();
		
		if (qc.hasErrors())
		{
			answer = "Failed to find POs";
			return;
		}
		
		if (chosen.isEmpty())
		{
			answer = "No POs in current " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class");
			return;
		}
		
		if (qc.initStrategies())
		{
			for (ProofObligation po: chosen)
			{
				long before = System.currentTimeMillis();
				StrategyResults results = qc.getValues(po);
				
				if (!qc.hasErrors())
				{
					ConsoleExecTimer execTimer = null;
					
					try
					{
						execTimer = new ConsoleExecTimer(timeout);
						execTimer.start();

						qc.checkObligation(po, results);

						if (includes.isEmpty() || includes.contains(po.status))
						{
							double duration = (double)(System.currentTimeMillis() - before)/1000;
							qc.printQuickCheckResult(po, duration, nominal);
						}
					}
					finally
					{
						if (execTimer != null)
						{
							execTimer.interrupt();
						}
					}
				}
				
				if (cancelled)
				{
					break;
				}
			}
		}
		
		answer = qc.hasErrors() ? "Failed" : cancelled ? "Cancelled" : "OK";
	}

	@Override
	protected void tail(double time) throws IOException
	{
		server.writeMessage(new DAPResponse(request, true, null,
				new JSONObject("result", answer, "variablesReference", 0)));
	}

	@Override
	protected void error(Throwable e) throws IOException
	{
		server.writeMessage(new DAPResponse(request, false, e.getMessage(), null));
		server.stdout("Execution terminated.");
	}

	@Override
	protected void clean() throws IOException
	{
		// Always kick the (LSP) client, since the PO statuses may have been updated...
		LSPServer lsp = LSPServer.getInstance();
		lsp.writeMessage(RPCRequest.notification("slsp/POG/updated",
				new JSONObject("successful", true)));
		
		running = null;
	}
	
	public String getAnswer()
	{
		return answer;
	}
}
