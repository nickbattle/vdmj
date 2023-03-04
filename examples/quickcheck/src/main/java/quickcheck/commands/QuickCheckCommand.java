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

import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.values.ValueList;

public class QuickCheckCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: quickcheck [-c <file>]|[-f <file>] [<PO numbers>]";
	public final static String HELP = "quickcheck - lightweight PO verification";
			
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
		String rangesFile = "ranges.qc";
		boolean createFile = false;
		List<Integer> poList = new Vector<Integer>();

		for (int i=1; i < argv.length; i++)
		{
			try
			{
				switch (argv[i])
				{
					case "-?":
					case "-help":
						return USAGE;
						
					case "-f":
						rangesFile = argv[++i];
						createFile = false;
						break;
						
					case "-c":
						if (++i < argv.length) rangesFile = argv[i];
						createFile = true;
						break;
						
					default:
						poList.add(Integer.parseInt(argv[i]));
						break;
				}
			}
			catch (NumberFormatException e)
			{
				println("Malformed PO#: " + e.getMessage());
				return USAGE;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				println("Missing argument");
				return USAGE;
			}
		}
		
		POPlugin po = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = po.getProofObligations();
		all.renumber();
		
		QuickCheck qc = new QuickCheck();
		ProofObligationList chosen = qc.getPOs(all, poList);

		if (chosen != null)
		{
			if (createFile)
			{
				qc.createRangeFile(rangesFile, chosen);
			}
			else
			{
				Map<String, ValueList> ranges = qc.readRangeFile(rangesFile);
				
				if (ranges != null)
				{
					qc.checkObligations(chosen, ranges);
				}
			}
		}
		
		return null;
	}
	
	public static void help()
	{
		println("quickcheck [-c <file>]|[-f <file>] [<PO numbers>] - lightweight PO verification");
	}
}
