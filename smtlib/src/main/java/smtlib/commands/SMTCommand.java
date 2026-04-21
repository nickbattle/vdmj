/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
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

package smtlib.commands;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.pog.POStatus;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import smtlib.SMTLIB;
import smtlib.ast.Script;

public class SMTCommand extends AnalysisCommand
{
	private final static String CMD = "smt [-s][-z3|-cvc5] <PO#> [<PO#>...]";
	private final static String SHORT = "smt [-s][-z3|-cvc5] <PO#> [<PO#>...]";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = SHORT + " - SMT solver PO verification";

	public SMTCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("smt"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			return "Only available for VDM-SL";
		}

		boolean source = false;
		String solver = SMTLIB.DEFAULT_SOLVER;
		List<Integer> numbers = new Vector<Integer>();

		for (String arg: argv)
		{
			if (arg.equals("smt"))
			{
				continue;
			}
			else if (arg.equals("-s"))
			{
				source = true;
			}
			else if (arg.equals("-cvc5") || arg.equals("-z3"))
			{
				solver = arg.toLowerCase();
			}
			else if (arg.equals("-s"))
			{
				source = true;
			}
			else
			{
				try
				{
					numbers.add(Integer.parseInt(arg));
				}
				catch (NumberFormatException e)
				{
					return USAGE;
				}
			}
		}

		if (numbers.isEmpty())
		{
			return USAGE;
		}

		POPlugin pog = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = pog.getProofObligations();

		try
		{
			TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
			SMTLIB smtlib = new SMTLIB(tc.getGlobalEnvironment(), solver);

			for (int number: numbers)
			{
				if (number < 1 || number > all.size())
				{
					return "No such PO: " + number;
				}

				ProofObligation po = all.get(number - 1);
				Script script = smtlib.generate(po);

				if (source)
				{
					Console.out.print(script.toSource());
					Console.out.println("----");
				}

				long before = System.currentTimeMillis();
				smtlib.runSolver(script, po);
				double duration = (double)(System.currentTimeMillis() - before)/1000;
				Console.out.print("PO #" + po.number + ", " + po.status.toString().toUpperCase());

				if (po.qualifier != null)
				{
					Console.out.printf(" %s", po.qualifier);
				}

				Console.out.println(" in " + duration + "s");

				if (po.getExplanation() != null)
				{
					Console.out.println(po.toTitle());
					Console.out.println(po.getExplanation());
				}
				else if (po.status == POStatus.FAILED)
				{
					Console.out.println(po.toTitle());
					Console.out.println(po.source);
				}
			}

			return null;
		}
		catch (IOException e)
		{
			return "Failed to execute solver: " + e.getMessage();
		}
		catch (UnsupportedOperationException e)
		{
			return "Failed: " + e.getMessage();
		}
	}
}
