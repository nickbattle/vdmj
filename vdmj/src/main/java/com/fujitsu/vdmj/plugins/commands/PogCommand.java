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

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.plural;
import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

public class PogCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: pog [<function/operation> | <number> | <status>]";

	public PogCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("pog"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		POPlugin po = registry.getPlugin("PO");
		ProofObligationList all = po.getProofObligations();
		ProofObligationList list = null;

		if (argv.length == 1)
		{
			list = all;
		}
		else
		{
    		String match = line.substring(line.indexOf(' ') + 1);
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

		if (list.isEmpty())
		{
			if (argv.length == 1)
			{
				println("No proof obligations generated");
			}
			else
			{
				println("Found no matching obligations");
			}
		}
		else
		{
			if (argv.length == 1)
			{
				println("Generated " + plural(list.size(), "proof obligation", "s") + ":\n");
			}
			else
			{
				println("Matched " + plural(list.size(), "proof obligation", "s") + ":\n");
			}

			printf("%s", list.toString());
		}
		
		return null;
	}
	
	public static void help()
	{
		println("pog [<function/operation/PO#>] - generate proof obligations");
	}
}
