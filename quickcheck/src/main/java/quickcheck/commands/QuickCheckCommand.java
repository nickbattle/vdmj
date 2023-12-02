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

import static quickcheck.commands.QCConsole.errorln;
import static quickcheck.commands.QCConsole.println;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleKeyWatcher;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import quickcheck.QuickCheck;
import quickcheck.strategies.QCStrategy;
import quickcheck.strategies.StrategyResults;

public class QuickCheckCommand extends AnalysisCommand
{
	private final static String CMD = "quickcheck [-?|-help][-q][-t <secs>][-s <strategy>]* [-<strategy:option>]* [<PO numbers/ranges/patterns>]";
	private final static String SHORT = "quickcheck [-help][<options>][<POs>]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = SHORT + " - lightweight PO verification";
			
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
		List<String> poNames = new Vector<String>();
		long timeout = 0;
		
		QuickCheck qc = new QuickCheck();

		List<String> arglist = new Vector<String>(Arrays.asList(argv));
		arglist.remove(0);	// "qc"
		qc.loadStrategies(arglist);
		
		if (qc.hasErrors())
		{
			errorln("Failed to load QC strategies");
			return null;
		}
		
		QCConsole.setQuiet(false);

		for (int i=0; i < arglist.size(); i++)	// Should just be POs, or -? -help
		{
			try
			{
				switch (arglist.get(i))
				{
					case "-?":
					case "-help":
						println(USAGE);
						println("Enabled strategies:");
						
						for (QCStrategy strategy: qc.getEnabledStrategies())
						{
							println("  " + strategy.help());
						}
						
						if (!qc.getDisabledStrategies().isEmpty())
						{
							println("Disabled strategies (add with -s <name>):");
							
							for (QCStrategy strategy: qc.getDisabledStrategies())
							{
								println("  " + strategy.help());
							}
						}
						
						return null;
						
					case "-q":
						QCConsole.setQuiet(true);
						break;
						
					case "-t":
						i++;
						timeout = Integer.parseInt(arglist.get(i));
						break;

					case "-":
						i++;
						int from = poList.get(poList.size() - 1);
						int to = Integer.parseInt(arglist.get(i));
						
						for (int po=from + 1; po <= to; po++)
						{
							poList.add(po);
						}
						break;
						
					default:
					{
						String arg = arglist.get(i);
						
						try
						{
							poList.add(Integer.parseInt(arg));
						}
						catch (NumberFormatException e)
						{
							if (arg.startsWith("-"))
							{
								errorln("Unexpected argument: " + arg);
								return USAGE;
							}
							
							poNames.add(arg);	// Name patterns
						}
						break;
					}
				}
			}
			catch (IndexOutOfBoundsException e)
			{
				errorln("Malformed arguments");
				return USAGE;
			}
			catch (NumberFormatException e)
			{
				errorln("Malformed argument: " + e.getMessage());
				return USAGE;
			}
		}
		
		POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = pog.getProofObligations();
		ProofObligationList chosen = qc.getPOs(all, poList, poNames);
		
		if (qc.hasErrors())
		{
			errorln("Failed to find POs");
			return null;
		}
		
		if (chosen.isEmpty())
		{
			errorln("No POs in current " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class"));
			return null;
		}
		
		if (qc.initStrategies(timeout))
		{
			for (ProofObligation po: chosen)
			{
				StrategyResults results = qc.getValues(po);
				
				if (!qc.hasErrors())
				{
					ConsoleKeyWatcher watcher = null;
					ConsoleDebugReader dbg = null;
					
					try
					{
						dbg = new ConsoleDebugReader();
						dbg.start();
						watcher = new ConsoleKeyWatcher(line);
						watcher.start();
						
						qc.checkObligation(po, results);
					}
					catch (Exception e)
					{
						errorln(e);
					}
					finally
					{
						if (watcher != null)
						{
							watcher.interrupt();
						}
						
						if (dbg != null)
						{
							dbg.interrupt();
						}
					}
				}
			}
		}
		
		return null;
	}
}
