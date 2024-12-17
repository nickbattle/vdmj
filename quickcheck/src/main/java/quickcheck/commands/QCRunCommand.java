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

import java.util.Map.Entry;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.config.Properties;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.plugins.commands.PrintCommand;
import com.fujitsu.vdmj.po.modules.MultiModuleEnvironment;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.pog.RecursiveObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;

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
					Context postate = null;
					
					if (obligation.definition != null)
					{
						if (obligation.counterexample != null)
						{
							if (obligation instanceof RecursiveObligation)
							{
								RecursiveObligation rec = (RecursiveObligation)obligation;
								
								if (rec.mutuallyRecursive)
								{
									return "Mutually recursive measures fail for these bindings: " +
											obligation.counterexample.toStringLine();
								}
							}
							
							launch = obligation.getCexLaunch();
							postate = obligation.getCexState();
						}
						else if (obligation.witness != null)
						{
							launch = obligation.getWitnessLaunch();
							postate = obligation.getWitnessState();
						}
						else
						{
							return "Obligation does not have a counterexample/witness. Run qc?";
						}
					}
					else if (obligation.kind.isStandAlone())
					{
						launch = obligation.getLaunch();
						postate = null;
					}
					else
					{
						return "Obligation does not have a callable definition?"; 
					}
					
					if (launch != null)
					{
						String pline = "print " + launch;
						println("=> " + pline);
						
						if (Settings.dialect == Dialect.VDM_SL && postate != null)
						{
							ModuleInterpreter m = ModuleInterpreter.getInstance();
							Context state = m.getStateContext();
							
							for (Entry<TCNameToken, Value> entry: postate.entrySet())
							{
								try
								{
									UpdatableValue value = (UpdatableValue) state.get(entry.getKey());
									value.set(LexLocation.ANY, entry.getValue(), m.getInitialContext());
								}
								catch (Exception e)
								{
									return "Problem setting state values for launch?";
								}
							}
						}
						
						// Temporarily allow maximal parsing, for invariant POs
						boolean saved = Properties.parser_maximal_types;

						try
						{
							Properties.parser_maximal_types = true;
							
							// Set the default Environment to allow complex launches to run which
							// use symbols outside the current module in VDM-SL. The default is
							// put back afterwards!
							
							Interpreter interpreter = Interpreter.getInstance();
							Environment menv = interpreter.getGlobalEnvironment();
							
							if (Settings.dialect == Dialect.VDM_SL)
							{
								POPlugin tc = registry.getPlugin("PO");
								menv = new MultiModuleEnvironment(tc.getPO());
							}
							
							PrintCommand cmd = new PrintCommand(pline, menv);
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
