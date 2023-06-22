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

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import quickcheck.QuickCheck;
import vdmj.commands.AnalysisCommand;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

public class QuickCheckLSPCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: quickcheck [-c <file>]|[-f <file>] [<PO numbers>]";
	public static final String HELP = "quickcheck - lightweight PO verification";
	
	public QuickCheckLSPCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("quickcheck") && !argv[0].equals("qc"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	private DAPMessageList result(DAPRequest request, String error)
	{
		if (error != null)
		{
			return new DAPMessageList(request, false, error, null);
		}
		else
		{
			return new DAPMessageList(request, new JSONObject("result", "OK"));
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		List<Integer> poList = new Vector<Integer>();
		QuickCheck qc = new QuickCheck();

		List<String> arglist = new Vector<String>(Arrays.asList(argv));
		arglist.remove(0);	// "qc"
		qc.loadPlugins(arglist);
		
		if (qc.hasErrors())
		{
			return result(request, "Failed to load QC plugins");
		}

		for (String arg: arglist)	// Should just be POs
		{
			try
			{
				switch (arg)
				{
					case "-?":
					case "-help":
						result(request, USAGE);
						
					default:
						poList.add(Integer.parseInt(arg));
						break;
				}
			}
			catch (NumberFormatException e)
			{
				println("Malformed PO#: " + e.getMessage());
				return result(request, USAGE);
			}
		}
		
		POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = pog.getProofObligations();
		all.renumber();
		ProofObligationList chosen = qc.getPOs(all, poList);
		
		if (qc.hasErrors())
		{
			return result(request, "Failed to find POs");
		}
		
		if (qc.initPlugins())
		{
			for (ProofObligation po: chosen)
			{
				qc.checkObligation(po, qc.getValues(po));
			}
		}
		
		return result(request, qc.hasErrors() ? "Failed" : null);
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}
}
