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
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
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

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.ExpressionExecutor;
import json.JSONObject;
import vdmj.commands.AnalysisCommand;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

/**
 * Launch a "print" command for a PO counterexample or witness.
 */
public class QCRunLSPCommand extends AnalysisCommand
{
	public static final String USAGE = "Usage: qcrun <PO number>";
	public static final String HELP = "qcrun <PO number> - execute counterexample/witness";

	private final int number;
	
	public QCRunLSPCommand(String line)
	{
		super(line);
		String[] parts = line.split("\\s+", 2);
		
		if (parts.length != 2)
		{
			throw new IllegalArgumentException(USAGE);
		}
		
		try
		{
			number = Integer.parseInt(argv[1]);
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		POPlugin po = PluginRegistry.getInstance().getPlugin("PO");
		ProofObligationList all = po.getProofObligations();

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
					return new DAPMessageList(request, e);		// Shouldn't happen
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
								return new DAPMessageList(request, new JSONObject("result",
									"Mutually recursive measures fail for these bindings: " +
									obligation.counterexample.toStringLine()));
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
						return new DAPMessageList(request, false,
							"Obligation does not have a counterexample/witness. Run qc?", null);
					}
				}
				else if (obligation.kind.isStandAlone())
				{
					launch = obligation.getLaunch();
					postate = null;
				}
				else
				{
					return new DAPMessageList(request, false,
							"Obligation does not have a callable definition?", null); 
				}
				
				if (launch != null)
				{
					println("=> print " + launch);

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
								return new DAPMessageList(request, false,
										"Problem setting state values for launch?", null);
							}
						}
					}

					// This allows maximal types to parse, for invariant POs, and allows POs to
					// reference types outside their module in VDM-SL.
					Environment env = (Settings.dialect == Dialect.VDM_SL) ?
						new MultiModuleEnvironment(po.getPO()) : null;
					
					ExpressionExecutor executor = new ExpressionExecutor("print", request, launch, true, env);
					executor.start();
					return null;
				}
				else
				{
					return new DAPMessageList(request, false,
						"Context does not bind all " + obligation.definition.name + " parameters?", null);
				}
			}
		}
		
		return new DAPMessageList(request, false, "No such obligation: " + number, null);
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}
}
