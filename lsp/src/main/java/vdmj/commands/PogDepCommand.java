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

package vdmj.commands;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import dap.DAPMessageList;
import dap.DAPRequest;
import workspace.PluginRegistry;
import workspace.plugins.POPlugin;

public class PogDepCommand extends AnalysisCommand
{
	private final static String CMD = "pogdep <[module`]name]>";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - list dependent POs";

	public PogDepCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("pogdep"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public DAPMessageList run(DAPRequest request)
	{
		POPlugin po = PluginRegistry.getInstance().getPlugin("PO");

		if (argv.length != 2)
		{
			return new DAPMessageList(request, false, USAGE, null);
		}

		String fname = argv[1];
		TCNameToken applyName = null;

		if (fname.contains("`"))
		{
			String[] parts = fname.split("`");

			if (parts.length != 2)
			{
				return new DAPMessageList(request, false, USAGE, null);
			}

			applyName = new TCNameToken(LexLocation.ANY, parts[0], parts[1]);
		}
		else
		{
			String def = Interpreter.getInstance().getDefaultName();
			applyName = new TCNameToken(LexLocation.ANY, def, fname);
		}

		if (!applyName.isReserved())
		{
			return new DAPMessageList(request, false, "Expecting constraint name (inv/pre/post)", null);
		}

		ProofObligationList list = po.getDependentPOs(applyName);

		if (list.isEmpty())
		{
			return new DAPMessageList(request, false, "No dependent POs", null);
		}

		// Use stdout, to match the QC command output format
		Console.out.print("Obligations dependent on " + applyName.toString() + ":\n\n");
		Console.out.print(list.toString());

		return new DAPMessageList(request);
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
}
