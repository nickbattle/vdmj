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

import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.values.ValueList;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
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
						return result(request, USAGE);
						
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
				errorln("Malformed PO#: " + e.getMessage());
				return result(request, USAGE);
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				errorln("Missing argument");
				return result(request, USAGE);
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
				qc.createRanges(rangesFile, chosen);
			}
			else
			{
				Map<String, ValueList> ranges = qc.readRanges(rangesFile);
				
				if (ranges != null)
				{
					qc.checkObligations(chosen, ranges);
				}
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