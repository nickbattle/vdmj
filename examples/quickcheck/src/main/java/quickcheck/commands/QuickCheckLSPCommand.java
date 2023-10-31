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

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import quickcheck.QuickCheck;
import quickcheck.strategies.QCStrategy;
import vdmj.commands.AnalysisCommand;
import vdmj.commands.InitRunnable;

public class QuickCheckLSPCommand extends AnalysisCommand implements InitRunnable
{
	public final static String CMD = "quickcheck [-?|-help][-t <secs>][-s <strategy>]* [-<strategy:option>]* [<PO numbers/ranges/patterns>]";
	private final static String USAGE = "Usage: " + CMD;
	
	private List<Integer> poList = new Vector<Integer>();
	private List<String> poNames = new Vector<String>();
	private long timeout = 0;
	private QuickCheck qc = new QuickCheck();
	
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

	private DAPMessageList setup(DAPRequest request)
	{
		List<String> arglist = new Vector<String>(Arrays.asList(argv));
		arglist.remove(0);	// "qc"
		qc.loadStrategies(arglist);
		
		if (qc.hasErrors())
		{
			return result(request, "Failed to load QC strategies");
		}

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
						
						return result(request, null);
						
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
								println("Unexpected argument: " + arg);
								return result(request, USAGE);
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
				return result(request, USAGE);
			}
			catch (NumberFormatException e)
			{
				println("Malformed argument: " + e.getMessage());
				return result(request, USAGE);
			}
		}
		
		return null;
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		DAPMessageList errs = setup(request);
		
		if (errs == null)
		{
			QuickCheckExecutor executor = new QuickCheckExecutor(request, qc, timeout, poList, poNames);
			executor.start();
		}
		
		return errs;
	}
	
	@Override
	public String initRun(DAPRequest request)
	{
		DAPMessageList errs = setup(request);
		
		if (errs == null)
		{
			try
			{
				QuickCheckExecutor executor = new QuickCheckExecutor(request, qc, timeout, poList, poNames);
				executor.exec();	// Note, not start!
			}
			catch (Exception e)
			{
				return e.getMessage();
			}
		}
		
		return "OK";
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

	@Override
	public String getExpression()
	{
		return "quickcheck";
	}

	@Override
	public String format(String result)
	{
		return result;
	}
}
