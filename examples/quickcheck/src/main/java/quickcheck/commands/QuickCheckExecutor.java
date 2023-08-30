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

import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import dap.AsyncExecutor;
import dap.DAPRequest;
import dap.DAPResponse;
import json.JSONObject;
import quickcheck.QuickCheck;
import quickcheck.qcplugins.Results;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

public class QuickCheckExecutor extends AsyncExecutor
{
	private final QuickCheck qc;
	private final List<Integer> poList;
	private String answer;

	public QuickCheckExecutor(DAPRequest request, QuickCheck qc, List<Integer> poList)
	{
		super("qc", request);
		this.qc = qc;
		this.poList = poList;
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
		all.renumber();
		ProofObligationList chosen = qc.getPOs(all, poList);
		
		if (qc.hasErrors())
		{
			answer = "Failed to find POs";
			return;
		}
		
		if (qc.initPlugins())
		{
			for (ProofObligation po: chosen)
			{
				Results results = qc.getValues(po);
				
				if (!qc.hasErrors())
				{
					qc.checkObligation(po, results);
				}
			}
		}
		
		answer = qc.hasErrors() ? "Failed" : "OK";
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
	protected void clean()
	{
		running = null;
	}
}
