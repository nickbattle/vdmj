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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class PogDepCommand extends AnalysisCommand
{
	private final static String CMD = "pogdep <[module`]name>";
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
	public String run(String line)
	{
		POPlugin po = registry.getPlugin("PO");

		if (argv.length != 2)
		{
			return USAGE;
		}

		String fname = argv[1];
		TCNameToken applyName = null;

		if (fname.contains("`"))
		{
			String[] parts = fname.split("`");

			if (parts.length != 2)
			{
				return USAGE;
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
			return "Expecting constraint name (inv/pre/post)";
		}

		ProofObligationList list = po.getDependentPOs(applyName);

		if (list.isEmpty())
		{
			return "No dependent POs";
		}

		printf("%s", list.toString());
		
		return null;
	}
}
