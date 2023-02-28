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

import static com.fujitsu.vdmj.plugins.PluginConsole.*;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.runtime.Interpreter;

public class DefaultCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: default <name>";
	private final static String KIND = Settings.dialect == Dialect.VDM_SL ? "module" : "class";
	
	public DefaultCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("default"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public String run(String line)
	{
		if (argv.length != 2)
		{
			return USAGE;
		}

		try
		{
			Interpreter.getInstance().setDefaultName(argv[1]);
			printf("Default %s set to %s\n", KIND, Interpreter.getInstance().getDefaultName());
		}
		catch (Exception e)
		{
			println(e.getMessage());	// Class/module not loaded
		}
		
		return null;
	}
	
	public static void help()
	{
		printf("default <%s> - set the default %s name\n", KIND, KIND);
	}
}
