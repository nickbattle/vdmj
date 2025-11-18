/*******************************************************************************
 *
 *	Copyright (c) 2025 Nick Battle.
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

package vdmj.commands;

import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.po.definitions.PODefinition;
import com.fujitsu.vdmj.pog.POContextStack;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import dap.DAPMessageList;
import dap.DAPRequest;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

public class PogCommand extends AnalysisCommand
{
	private final static String CMD = "pog [<fn/op name> | <number> | <status>]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - generate proof obligations";

	public PogCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("pog"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		POPlugin po = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = po.getProofObligations();
		ProofObligationList list = null;

		if (argv.length == 1)
		{
			list = all;
		}
		else if (argv.length == 2)
		{
    		String match = argv[1];
			list = new ProofObligationList();

			for (ProofObligation obligation: all)
			{
				if (obligation.name.startsWith(match) ||
					Integer.toString(obligation.number).equals(match) ||
					obligation.status.toString().equalsIgnoreCase(match))
				{
					list.add(obligation);
				}
			}
		}
		else
		{
			return new DAPMessageList(request, false, USAGE, null);
		}

		if (list.isEmpty())
		{
			if (argv.length == 1)
			{
				return new DAPMessageList(request, false, "No proof obligations generated", null);
			}
			else
			{
				return new DAPMessageList(request, false, "Found no matching obligations", null);
			}
		}
		else
		{
			StringBuilder sb = new StringBuilder();

			if (argv.length == 1)
			{
				sb.append("Generated " + plural(list.size(), "proof obligation", "s") + ":\n");
			}
			else
			{
				sb.append("Matched " + plural(list.size(), "proof obligation", "s") + ":\n");
			}

			sb.append("\n");
			sb.append(list.toString());

			// Use stdout, to match the QC command output format
			Console.out.print(sb.toString());

			if (argv.length == 1)
			{
				for (PODefinition def: POContextStack.getReducedDefinitions())
				{
					Console.out.printf("POs missing for %s (%d paths)\n",
						def.name.getExplicit(true), def.getAlternativePaths());
				}
			}

			return new DAPMessageList(request);
		}
	}

	private String plural(int n, String s, String pl)
	{
		return n + " " + (n != 1 ? s + pl : s);
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}

	@Override
	public boolean notWhenDirty()
	{
		return true;
	}
}
