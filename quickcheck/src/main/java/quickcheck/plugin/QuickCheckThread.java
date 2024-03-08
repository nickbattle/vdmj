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

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import json.JSONObject;
import lsp.CancellableThread;
import lsp.LSPServer;
import quickcheck.QuickCheck;
import quickcheck.strategies.StrategyResults;
import rpc.RPCRequest;
import workspace.Diag;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

public class QuickCheckThread extends CancellableThread
{
	private final QuickCheck qc;
	private final long timeout;
	private final List<Integer> poList;
	private final List<String> poNames;

	public QuickCheckThread(RPCRequest request, QuickCheck qc, long timeout, List<Integer> poList, List<String> poNames)
	{
		super(request.get("id"));
		this.qc = qc;
		this.timeout = timeout;
		this.poList = poList;
		this.poNames = poNames;
	}

	@Override
	protected void body()
	{
		try
		{
			running = "quickcheck";
			
			POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
			ProofObligationList all = pog.getProofObligations();
			ProofObligationList chosen = qc.getPOs(all, poList, poNames);
			
			if (qc.hasErrors())
			{
				Diag.error("Failed to find POs");
				LSPServer.getInstance().writeMessage(
						RPCRequest.notification("slsp/POG/updated",
							new JSONObject("quickcheck", false)));
				return;
			}
			
			if (chosen.isEmpty())
			{
				Diag.error("No POs in current " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class"));
				LSPServer.getInstance().writeMessage(
						RPCRequest.notification("slsp/POG/updated",
							new JSONObject("quickcheck", false)));
				return;
			}
			
			if (qc.initStrategies(timeout))
			{
				for (ProofObligation po: chosen)
				{
					StrategyResults results = qc.getValues(po);
					
					if (!qc.hasErrors())
					{
						qc.checkObligation(po, results);
					}
					
					if (cancelled)
					{
						break;
					}
				}
			}
		
			LSPServer.getInstance().writeMessage(
				RPCRequest.notification("slsp/POG/updated",
					new JSONObject("quickcheck", !qc.hasErrors())));
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
}
