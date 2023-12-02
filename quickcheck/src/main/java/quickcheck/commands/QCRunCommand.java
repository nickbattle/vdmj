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

import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.plugins.commands.PrintCommand;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Interpreter;

/**
 * Launch a "print" command for a PO counterexample or witness.
 */
public class QCRunCommand extends AnalysisCommand
{
	private final static String CMD = "qcrun <PO number>";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - execute counterexample/witness";

	public QCRunCommand(String line)
	{
		super(line);

		if (!argv[0].equals("qcrun") && !argv[0].equals("qr"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		POPlugin po = registry.getPlugin("PO");
		ProofObligationList all = po.getProofObligations();

		if (argv.length == 2)
		{
    		int number = 0;
    		
			try
			{
				number = Integer.parseInt(argv[1]);
			}
			catch (NumberFormatException e)
			{
				return USAGE;
			}

			for (ProofObligation obligation: all)
			{
				if (obligation.number == number)
				{
					try
					{
						Interpreter.getInstance().setDefaultName(obligation.location.module);
					}
					catch (Exception e)
					{
						return "Exception: " + e.getMessage();		// Shouldn't happen
					}
					
					String launch = null;
					
					if (!obligation.counterexample.isEmpty())
					{
						launch = obligation.getCexLaunch();
					}
					else if (!obligation.witness.isEmpty())
					{
						launch = obligation.getWitnessLaunch();
					}
					else
					{
						return "Obligation does not have a counterexample/witness. Run qc?";
					}
					
					if (launch != null)
					{
						String pline = "print " + launch;
						println("=> " + pline);
						
						// Temporarily allow maximal parsing, for invariant POs
						boolean saved = Properties.parser_maximal_types;
						
						try
						{
							Properties.parser_maximal_types = true;
							PrintCommand cmd = new PrintCommand(pline);
							return cmd.run(pline);
						}
						finally
						{
							Properties.parser_maximal_types = saved;
						}
					}
					else
					{
						return "Context does not bind all " + obligation.definition.name + " parameters?";
					}
				}
			}
			
			return "No such obligation: " + number;
		}
		else
		{
			return USAGE;
		}
	}
}
