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

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static quickcheck.commands.QCConsole.verbose;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.debug.ConsoleDebugReader;
import com.fujitsu.vdmj.debug.ConsoleExecTimer;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import quickcheck.QuickCheck;
import quickcheck.strategies.StrategyResults;

public class QuickCheckCommand extends AnalysisCommand
{
	private final static String CMD = "quickcheck [-?|-help][-q|-v|-n][-t <secs>][-i <status>]*[-s <strategy>]* [-<strategy:option>]* [<PO numbers/ranges/patterns>]";
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
		List<POStatus> includes = new Vector<POStatus>();
		long timeout = -1;
		boolean numbersOnly = false;
		
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
		QCConsole.setVerbose(false);

		for (int i=0; i < arglist.size(); i++)	// Should just be POs, or -? -help
		{
			try
			{
				switch (arglist.get(i))
				{
					case "-?":
					case "-help":
						qc.printHelp(USAGE);
						return null;
						
					case "-q":
						QCConsole.setQuiet(true);
						break;
						
					case "-v":
						QCConsole.setVerbose(true);
						break;
						
					case "-t":
						i++;
						timeout = Integer.parseInt(arglist.get(i));
						break;
						
					case "-i":
						try
						{
							i++;
							includes.add(POStatus.valueOf(arglist.get(i).toUpperCase()));
						}
						catch (IllegalArgumentException e)
						{
							println("Not a valid PO status: " + arglist.get(i));
							return USAGE;
						}
						break;
						
					case "-n":
						numbersOnly = true;
						QCConsole.setQuiet(true);
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
								println("Unexpected argument: " + arg);
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
				println("Malformed arguments");
				return USAGE;
			}
			catch (NumberFormatException e)
			{
				println("Malformed argument: " + e.getMessage());
				return USAGE;
			}
		}

		QCConsole.setIncludes(includes);

		POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = pog.getProofObligations();
		ProofObligationList chosen = qc.getPOs(all, poList, poNames);
		
		if (qc.hasErrors())
		{
			println("Failed to find POs");
			return null;
		}
		
		if (chosen.isEmpty())
		{
			println("No POs in current " + (Settings.dialect == Dialect.VDM_SL ? "module" : "class"));
			return null;
		}

		timeout = (timeout < 0) ? QuickCheck.DEFAULT_TIMEOUT : timeout;

		if (qc.initStrategies())
		{
			for (ProofObligation po: chosen)
			{
				verbose("Processing PO #%s\n", po.number);
				StrategyResults results = qc.getValues(po);
				
				if (!qc.hasErrors())
				{
					ConsoleExecTimer execTimer = null;
					ConsoleDebugReader dbg = null;
					
					try
					{
						dbg = new ConsoleDebugReader();
						dbg.start();
						execTimer = new ConsoleExecTimer(timeout);
						execTimer.start();
						
						qc.checkObligation(po, results);
						
						if (numbersOnly)
						{
							if (includes.isEmpty() || includes.contains(po.status))
							{
								Console.out.printf("PO #%d: %s\n", po.number, po.status.toString().toUpperCase());
							}
						}
					}
					catch (Exception e)
					{
						errorln(e);
					}
					finally
					{
						if (execTimer != null)
						{
							execTimer.interrupt();
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
