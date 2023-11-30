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

package com.fujitsu.vdmj.plugins.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.ClassInterpreter;

public class CreateCommand extends AnalysisCommand
{
	private final static String CMD = "create <name> := <expression>";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - create a named variable";

	public CreateCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("create"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (Settings.dialect == Dialect.VDM_SL)
		{
			return "Command is not available in VDM-SL";
		}
		
		Pattern p = Pattern.compile("^create (\\w+)\\s*?:=\\s*(.+)$");
		Matcher m = p.matcher(line);

		if (m.matches())
		{
			String var = m.group(1);
			String exp = m.group(2);

			try
			{
				ClassInterpreter cinterpreter = ClassInterpreter.getInstance();
				cinterpreter.create(var, exp);
			}
			catch (Exception e)
			{
				println(e.getMessage());
			}
		}
		else
		{
			println(USAGE);
		}
		
		return null;
	}
}
