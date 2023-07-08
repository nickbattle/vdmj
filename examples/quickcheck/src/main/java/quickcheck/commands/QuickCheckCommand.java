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
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.QuickCheck;
import quickcheck.qcplugins.QCPlugin;

public class QuickCheckCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: quickcheck [-?|-help][-p <name>]* [<plugin options>] [<PO numbers>]";
			
	public QuickCheckCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("quickcheck") && !argv[0].equals("qc"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		List<Integer> poList = new Vector<Integer>();
		QuickCheck qc = new QuickCheck();

		List<String> arglist = new Vector<String>(Arrays.asList(argv));
		arglist.remove(0);	// "qc"
		qc.loadPlugins(arglist);
		
		if (qc.hasErrors())
		{
			return "Failed to load QC plugins";
		}

		for (String arg: arglist)	// Should just be POs
		{
			try
			{
				switch (arg)
				{
					case "-?":
					case "-help":
						println(USAGE);
						
						for (QCPlugin plugin: qc.getPlugins())
						{
							println(plugin.help());
						}
						return null;
						
					default:
						poList.add(Integer.parseInt(arg));
						break;
				}
			}
			catch (NumberFormatException e)
			{
				println("Malformed PO#: " + e.getMessage());
				return USAGE;
			}
		}
		
		POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = pog.getProofObligations();
		all.renumber();
		ProofObligationList chosen = qc.getPOs(all, poList);
		
		if (qc.hasErrors())
		{
			return "Failed to find POs";
		}
		
		if (qc.initPlugins())
		{
			for (ProofObligation po: chosen)
			{
				Map<String, ValueSet> values = qc.getValues(po);
				
				if (!qc.hasErrors())
				{
					qc.checkObligation(po, values);
				}
			}
		}
		
		return null;
	}
	
	public static void help()
	{
		println("quickcheck [-?|-help][-p <names>]* [<plugin options>] [<PO numbers>] - lightweight PO verification");
	}
}
