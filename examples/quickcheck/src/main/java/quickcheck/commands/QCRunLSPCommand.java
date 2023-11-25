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

import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;

import dap.DAPMessageList;
import dap.DAPRequest;
import dap.ExpressionExecutor;
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
					return new DAPMessageList(request, false,
						"Obligation does not have a counterexample/witness. Run qc?", null);
				}
				
				if (launch != null)
				{
					println("=> print " + launch);
					ExpressionExecutor executor = new ExpressionExecutor("print", request, launch);
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
